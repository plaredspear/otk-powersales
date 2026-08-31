package com.otoki.powersales.platform.push.sender

import com.google.api.client.json.gson.GsonFactory
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import com.google.firebase.messaging.SendResponse
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.push.config.FcmProperties
import com.otoki.powersales.platform.push.service.FcmTokenService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream

/**
 * Firebase Admin SDK(HTTP v1) 기반 실제 FCM 발송 (운영 `!local`).
 *
 * credential 은 [FcmProperties.credentialS3Key] 가 가리키는 S3 객체(Firebase 서비스 계정 키 JSON)에서
 * 최초 발송 시 lazy 로 로드한다. 미설정/비활성/S3 부재/초기화 실패 시 발송을 graceful 하게
 * skip([FcmSendResult.EMPTY]) 한다 — 부팅/배치를 깨뜨리지 않는다.
 * 대상은 HTTP v1 batch 상한(500) 단위로 분할 발송한다.
 *
 * 배지([PushTarget.badge])는 대상마다 값이 다르므로, 동일 payload 를 공유하는 multicast 대신
 * 대상별 [Message] 를 만들어 `sendEach` 로 보낸다 (HTTP 호출 수는 multicast 와 동일하게 500건당 1회).
 *
 * 발송 응답이 `UNREGISTERED` 인 토큰(앱 삭제/기기 초기화/강제 로그아웃 후 단말이 폐기한 토큰)은
 * 다시는 도달하지 않으므로 발송 직후 저장소에서 정리한다([FcmTokenService.clearUnregisteredTokens]).
 * 호출처가 아니라 발송기에서 처리하는 이유는, 발송 지점이 늘어나도 정리가 누락되지 않게 하기
 * 위함이다 (FCM 응답을 보는 곳은 여기뿐이다).
 */
@Component
@Profile("!local")
class RealFcmSender(
    private val properties: FcmProperties,
    private val storageService: StorageService,
    private val fcmTokenService: FcmTokenService,
) : FcmSender {

    private val log = LoggerFactory.getLogger(javaClass)

    @Volatile
    private var messaging: FirebaseMessaging? = null

    @Volatile
    private var initialized = false

    override fun sendNotification(
        targets: List<PushTarget>,
        title: String,
        body: String,
        data: Map<String, String>,
    ): FcmSendResult {
        if (targets.isEmpty()) return FcmSendResult.EMPTY
        val messaging = resolveMessaging() ?: return FcmSendResult.EMPTY

        var success = 0
        var failure = 0
        val unregistered = mutableListOf<String>()
        targets.chunked(BATCH_LIMIT).forEach { chunk ->
            try {
                val messages = chunk.map { buildMessage(it, title, body, data) }
                logPayloadSample(messages.first())
                val response = messaging.sendEach(messages)
                success += response.successCount
                failure += response.failureCount
                unregistered += unregisteredTokensOf(chunk, response.responses)
            } catch (e: Exception) {
                log.error("FCM 발송 실패 (chunk size=${chunk.size})", e)
                failure += chunk.size
            }
        }
        // 죽은 토큰을 남겨두면 해당 사원이 계속 발송 대상에 잡혀 실패만 누적된다. 정리 실패가
        // 발송 결과 자체를 뒤엎지 않도록 예외는 흡수한다(다음 발송에서 다시 정리된다).
        try {
            fcmTokenService.clearUnregisteredTokens(unregistered)
        } catch (e: Exception) {
            log.error("FCM 무효 토큰 정리 실패 (count=${unregistered.size})", e)
        }
        return FcmSendResult(
            successCount = success,
            failureCount = failure,
            unregisteredTokens = unregistered,
        )
    }

    /**
     * 발송 직전 payload 1건을 DEBUG 로 남긴다 — "알림은 오는데 소리/진동이 없다" 류의 신고에서
     * 서버가 실제로 무엇을 보냈는지(특히 `aps.sound`)를 코드 추측이 아니라 로그로 확정하기 위함이다.
     *
     * SDK 가 wire 에 싣는 형태 그대로를 보려고 `@Key` 기반 직렬화를 쓴다. 토큰은 인증정보라 제외되도록
     * 앞 12자만 남기고, 직렬화 실패는 발송을 막지 않는다(로그 목적이므로 조용히 무시).
     * DEBUG 레벨이라 운영 기본 설정에서는 출력되지 않는다.
     */
    private fun logPayloadSample(message: Message) {
        if (!log.isDebugEnabled) return
        try {
            val json = GsonFactory.getDefaultInstance().toString(message)
            log.debug("FCM payload 샘플: {}", json.replace(TOKEN_JSON_REGEX, "\"token\":\"<masked>\""))
        } catch (e: Exception) {
            log.debug("FCM payload 직렬화 실패(무시): {}", e.message)
        }
    }

    /**
     * batch 응답에서 "더 이상 존재하지 않는 토큰"만 골라낸다.
     *
     * 응답은 요청 메시지와 같은 순서로 돌아오므로 index 로 원 토큰을 되짚는다.
     *
     * `UNREGISTERED` 만 정리 대상으로 삼는다 — `INVALID_ARGUMENT` 는 토큰 형식 오류뿐 아니라
     * **payload 오류** 로도 발생하는데, 후자라면 모든 대상이 동시에 그 코드로 실패하므로
     * 정리 대상에 넣으면 멀쩡한 토큰까지 전부 지워버린다. 진단을 위해 로그만 남긴다.
     *
     * (이 판별 규칙만 단위 테스트로 고정할 수 있게 `internal` 로 노출한다 — 발송 경로 전체는
     * Firebase SDK 실호출이라 테스트가 불가능하다.)
     */
    internal fun unregisteredTokensOf(
        chunk: List<PushTarget>,
        responses: List<SendResponse>,
    ): List<String> {
        val unregistered = mutableListOf<String>()
        var invalidArgument = 0
        responses.forEachIndexed { index, sendResponse ->
            if (sendResponse.isSuccessful) return@forEachIndexed
            when (sendResponse.exception?.messagingErrorCode) {
                MessagingErrorCode.UNREGISTERED -> chunk.getOrNull(index)?.let { unregistered += it.token }
                MessagingErrorCode.INVALID_ARGUMENT -> invalidArgument++
                else -> Unit
            }
        }
        if (invalidArgument > 0) {
            log.warn("FCM INVALID_ARGUMENT ${invalidArgument}건 — 토큰 형식 또는 payload 확인 필요(정리 대상 아님)")
        }
        return unregistered
    }

    /**
     * 대상 1건의 발송 메시지를 만든다.
     *
     * iOS 알림음/진동은 `aps.sound` 가 결정한다 — 이 키가 없으면 iOS 는 배너만 조용히 띄우고
     * **진동도 하지 않는다**(진동만 켜는 APNs 옵션은 없다. 사운드 지정 시 무음 모드에서 진동,
     * 벨소리 모드에서 사운드+진동이며 최종 동작은 사용자의 시스템 설정을 따른다).
     * 앱의 [setForegroundNotificationPresentationOptions] 나 권한의 `sound: true` 는 "재생 허용"
     * 일 뿐 재생 여부를 만들지 않으므로, 배지 유무와 무관하게 항상 실어 Android 채널
     * (`Importance.high`, 기본 진동)과 동작을 맞춘다.
     *
     * 배지는 지정된 경우에만 싣는다:
     * - iOS: `aps.badge` — 절대값(증분 아님). 키를 싣지 않으면 기기 배지는 변하지 않는다.
     *   alert(title/body)는 FCM 이 상위 notification 을 aps.alert 로 변환해 채운다.
     * - Android: `notification_count` — 런처 배지 숫자(지원 런처 한정). 미지정 시 활성 알림 수 기준 기본 동작.
     *
     * (payload 구성만 단위 테스트로 고정할 수 있게 `internal` 로 노출한다 — 발송 경로 전체는
     * Firebase SDK 실호출이라 테스트가 불가능하다.)
     */
    internal fun buildMessage(
        target: PushTarget,
        title: String,
        body: String,
        data: Map<String, String>,
    ): Message {
        val aps = Aps.builder()
            .setSound(DEFAULT_APNS_SOUND)
            .apply { target.badge?.let { setBadge(it) } }
            .build()

        val builder = Message.builder()
            .setToken(target.token)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .putAllData(data)
            .setApnsConfig(ApnsConfig.builder().setAps(aps).build())

        target.badge?.let { badge ->
            builder.setAndroidConfig(
                AndroidConfig.builder()
                    .setNotification(
                        AndroidNotification.builder().setNotificationCount(badge).build()
                    )
                    .build()
            )
        }
        return builder.build()
    }

    /** lazy 초기화. credential 미설정/실패 시 null (no-op). */
    private fun resolveMessaging(): FirebaseMessaging? {
        if (initialized) return messaging
        synchronized(this) {
            if (initialized) return messaging
            messaging = try {
                initMessaging()
            } catch (e: Exception) {
                log.error("FCM 초기화 실패 — 발송을 skip 합니다.", e)
                null
            }
            initialized = true
            return messaging
        }
    }

    private fun initMessaging(): FirebaseMessaging? {
        if (!properties.enabled) {
            log.warn("FCM 발송 비활성 (app.push.fcm.enabled=false) — 발송 skip.")
            return null
        }
        val s3Key = properties.credentialS3Key?.takeIf { it.isNotBlank() }
        if (s3Key == null) {
            log.warn("FCM credential S3 key(app.push.fcm.credential-s3-key) 미설정 — 발송 skip.")
            return null
        }
        // Firebase 서비스 계정 키 JSON 을 비공개 S3 객체에서 로드 (EB 인스턴스 IAM 접근).
        val credentialBytes = storageService.download(s3Key)
        val credentials = GoogleCredentials.fromStream(ByteArrayInputStream(credentialBytes))
        val options = FirebaseOptions.builder().setCredentials(credentials).build()
        val app = FirebaseApp.getApps().firstOrNull { it.name == APP_NAME }
            ?: FirebaseApp.initializeApp(options, APP_NAME)
        return FirebaseMessaging.getInstance(app)
    }

    companion object {
        private const val APP_NAME = "otoki-fcm"

        /** FCM HTTP v1 batch(sendEach) 1회 호출 메시지 상한. */
        private const val BATCH_LIMIT = 500

        /** iOS 알림음/진동을 켜는 `aps.sound` 값 — 기기 기본 알림음. */
        internal const val DEFAULT_APNS_SOUND = "default"

        /** payload 로그에서 기기 토큰을 가리기 위한 패턴 (토큰은 인증정보다). */
        private val TOKEN_JSON_REGEX = Regex(""""token"\s*:\s*"[^"]*"""")
    }
}
