package com.otoki.powersales.platform.push.sender

import com.google.api.client.json.gson.GsonFactory
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.SendResponse
import com.otoki.powersales.platform.common.storage.StorageNotFoundException
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.push.config.FcmProperties
import com.otoki.powersales.platform.push.service.FcmTokenService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RealFcmSender — S3 credential 로딩 / 게이팅")
class RealFcmSenderTest {

    private val storageService: StorageService = mockk()
    private val fcmTokenService: FcmTokenService = mockk(relaxed = true)

    private fun sender(enabled: Boolean, s3Key: String?): RealFcmSender =
        RealFcmSender(
            properties = FcmProperties(enabled = enabled, credentialS3Key = s3Key),
            storageService = storageService,
            fcmTokenService = fcmTokenService,
        )

    @Nested
    @DisplayName("발송 게이팅 — credential 미충족 시 no-op(EMPTY) + S3 미접근")
    inner class Gating {

        @Test
        @DisplayName("빈 토큰 목록이면 EMPTY (S3 접근 없음)")
        fun emptyTokens() {
            val result = sender(enabled = true, s3Key = "config/fcm/key.json")
                .sendNotification(emptyList(), "t", "b", emptyMap())

            assertThat(result).isEqualTo(FcmSendResult.EMPTY)
            verify(exactly = 0) { storageService.download(any()) }
        }

        @Test
        @DisplayName("enabled=false 면 EMPTY + S3 미접근")
        fun disabled() {
            val result = sender(enabled = false, s3Key = "config/fcm/key.json")
                .sendNotification(listOf(PushTarget("token-a")), "t", "b", emptyMap())

            assertThat(result).isEqualTo(FcmSendResult.EMPTY)
            verify(exactly = 0) { storageService.download(any()) }
        }

        @Test
        @DisplayName("credential-s3-key blank 면 EMPTY + S3 미접근")
        fun blankS3Key() {
            val result = sender(enabled = true, s3Key = "  ")
                .sendNotification(listOf(PushTarget("token-a")), "t", "b", emptyMap())

            assertThat(result).isEqualTo(FcmSendResult.EMPTY)
            verify(exactly = 0) { storageService.download(any()) }
        }

        @Test
        @DisplayName("credential-s3-key null 이면 EMPTY + S3 미접근")
        fun nullS3Key() {
            val result = sender(enabled = true, s3Key = null)
                .sendNotification(listOf(PushTarget("token-a")), "t", "b", emptyMap())

            assertThat(result).isEqualTo(FcmSendResult.EMPTY)
            verify(exactly = 0) { storageService.download(any()) }
        }
    }

    @Nested
    @DisplayName("S3 로딩 — enabled + key 충족 시 해당 key 로 S3 다운로드 시도")
    inner class S3Load {

        @Test
        @DisplayName("S3 객체 부재(StorageNotFoundException) → graceful skip(EMPTY), 예외 미전파")
        fun s3ObjectMissing() {
            every { storageService.download("config/fcm/key.json") } throws
                StorageNotFoundException("config/fcm/key.json")

            val result = sender(enabled = true, s3Key = "config/fcm/key.json")
                .sendNotification(listOf(PushTarget("token-a")), "t", "b", emptyMap())

            assertThat(result).isEqualTo(FcmSendResult.EMPTY)
            verify(exactly = 1) { storageService.download("config/fcm/key.json") }
        }

        @Test
        @DisplayName("S3 객체가 유효하지 않은 JSON → credential 파싱 실패해도 graceful skip(EMPTY)")
        fun s3ObjectInvalidJson() {
            every { storageService.download("config/fcm/key.json") } returns
                "not a valid service account json".toByteArray()

            val result = sender(enabled = true, s3Key = "config/fcm/key.json")
                .sendNotification(listOf(PushTarget("token-a")), "t", "b", emptyMap())

            // credential 파싱/초기화 실패는 resolveMessaging 에서 catch → EMPTY (부팅/발송 미중단)
            assertThat(result).isEqualTo(FcmSendResult.EMPTY)
            verify(exactly = 1) { storageService.download("config/fcm/key.json") }
        }

        @Test
        @DisplayName("초기화 1회 실패 후 재발송 시 재초기화하지 않음(lazy 캐시)")
        fun initOnce() {
            every { storageService.download("config/fcm/key.json") } returns
                "invalid".toByteArray()
            val s = sender(enabled = true, s3Key = "config/fcm/key.json")

            s.sendNotification(listOf(PushTarget("token-a")), "t", "b", emptyMap())
            s.sendNotification(listOf(PushTarget("token-b")), "t", "b", emptyMap())

            // resolveMessaging 이 initialized 플래그로 1회만 초기화 → S3 다운로드도 1회
            verify(exactly = 1) { storageService.download("config/fcm/key.json") }
        }
    }

    @Nested
    @DisplayName("무효 토큰 판별 — UNREGISTERED 만 정리 대상")
    inner class UnregisteredDetection {

        private fun failure(code: MessagingErrorCode): SendResponse = mockk {
            every { isSuccessful } returns false
            every { exception } returns mockk { every { messagingErrorCode } returns code }
        }

        private fun success(): SendResponse = mockk {
            every { isSuccessful } returns true
        }

        @Test
        @DisplayName("UNREGISTERED 응답의 토큰만 index 로 되짚어 수집한다")
        fun collectsUnregisteredByIndex() {
            val chunk = listOf(PushTarget("live"), PushTarget("dead"), PushTarget("also-dead"))
            val responses = listOf(
                success(),
                failure(MessagingErrorCode.UNREGISTERED),
                failure(MessagingErrorCode.UNREGISTERED),
            )

            val result = sender(enabled = true, s3Key = "k").unregisteredTokensOf(chunk, responses)

            assertThat(result).containsExactly("dead", "also-dead")
        }

        @Test
        @DisplayName("INVALID_ARGUMENT / 일시 오류는 정리 대상이 아니다")
        fun ignoresNonUnregisteredFailures() {
            val chunk = listOf(PushTarget("a"), PushTarget("b"), PushTarget("c"))
            val responses = listOf(
                // payload 오류로도 발생하는 코드 — 정리하면 멀쩡한 토큰까지 지워진다.
                failure(MessagingErrorCode.INVALID_ARGUMENT),
                failure(MessagingErrorCode.UNAVAILABLE),
                failure(MessagingErrorCode.INTERNAL),
            )

            val result = sender(enabled = true, s3Key = "k").unregisteredTokensOf(chunk, responses)

            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("발송 payload — iOS 알림음/진동(aps.sound) + 배지")
    inner class PayloadBuilding {

        /**
         * SDK 가 실제로 wire 에 싣는 JSON 으로 검증한다 — [Message] 의 getter 는 package-private 이라
         * 필드를 직접 못 읽고, `@Key` 어노테이션 기반 직렬화가 곧 전송 형태이기 때문이다.
         */
        @Suppress("UNCHECKED_CAST")
        private fun payloadOf(target: PushTarget): Map<String, Any?> {
            val message: Message = sender(enabled = true, s3Key = "k")
                .buildMessage(target, "제목", "본문", mapOf("type" to "NOTICE"))
            val json = GsonFactory.getDefaultInstance().toString(message)
            return GsonFactory.getDefaultInstance()
                .fromString(json, Map::class.java) as Map<String, Any?>
        }

        @Suppress("UNCHECKED_CAST")
        private fun nested(map: Map<String, Any?>, key: String): Map<String, Any?> =
            map[key] as Map<String, Any?>

        private fun apsOf(payload: Map<String, Any?>): Map<String, Any?> =
            nested(nested(nested(payload, "apns"), "payload"), "aps")

        @Test
        @DisplayName("배지가 없어도 aps.sound=default 를 싣는다 (iOS 무음 배너 → 알림음/진동)")
        fun soundWithoutBadge() {
            val aps = apsOf(payloadOf(PushTarget("token-a")))

            assertThat(aps["sound"]).isEqualTo(RealFcmSender.DEFAULT_APNS_SOUND)
            // 배지 미지정 시 키 자체를 빼야 기기 배지가 0 으로 덮이지 않는다.
            assertThat(aps).doesNotContainKey("badge")
        }

        @Test
        @DisplayName("배지가 있으면 aps 에 sound 와 badge 가 함께 실린다")
        fun soundWithBadge() {
            val aps = apsOf(payloadOf(PushTarget("token-a", badge = 3)))

            assertThat(aps["sound"]).isEqualTo(RealFcmSender.DEFAULT_APNS_SOUND)
            assertThat((aps["badge"] as Number).toInt()).isEqualTo(3)
        }

        @Test
        @DisplayName("Android 배지(notification_count)는 배지 지정 시에만 싣는다")
        fun androidNotificationCount() {
            assertThat(payloadOf(PushTarget("token-a"))).doesNotContainKey("android")

            val android = nested(payloadOf(PushTarget("token-a", badge = 3)), "android")
            val notification = nested(android, "notification")
            assertThat((notification["notification_count"] as Number).toInt()).isEqualTo(3)
        }
    }
}
