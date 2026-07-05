package com.otoki.powersales.platform.push.sender

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.push.config.FcmProperties
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
 * 토큰은 HTTP v1 multicast 상한(500) 단위로 분할 발송한다.
 */
@Component
@Profile("!local")
class RealFcmSender(
    private val properties: FcmProperties,
    private val storageService: StorageService,
) : FcmSender {

    private val log = LoggerFactory.getLogger(javaClass)

    @Volatile
    private var messaging: FirebaseMessaging? = null

    @Volatile
    private var initialized = false

    override fun sendNotificationToTokens(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String>,
    ): FcmSendResult {
        if (tokens.isEmpty()) return FcmSendResult.EMPTY
        val messaging = resolveMessaging() ?: return FcmSendResult.EMPTY

        var success = 0
        var failure = 0
        tokens.chunked(MULTICAST_LIMIT).forEach { chunk ->
            try {
                val message = MulticastMessage.builder()
                    .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                    .putAllData(data)
                    .addAllTokens(chunk)
                    .build()
                val response = messaging.sendEachForMulticast(message)
                success += response.successCount
                failure += response.failureCount
            } catch (e: Exception) {
                log.error("FCM 발송 실패 (chunk size=${chunk.size})", e)
                failure += chunk.size
            }
        }
        return FcmSendResult(successCount = success, failureCount = failure)
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

        /** FCM HTTP v1 multicast 1회 호출 토큰 상한. */
        private const val MULTICAST_LIMIT = 500
    }
}
