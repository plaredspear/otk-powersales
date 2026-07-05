package com.otoki.powersales.platform.push.sender

import com.otoki.powersales.platform.common.storage.StorageNotFoundException
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.push.config.FcmProperties
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

    private fun sender(enabled: Boolean, s3Key: String?): RealFcmSender =
        RealFcmSender(
            properties = FcmProperties(enabled = enabled, credentialS3Key = s3Key),
            storageService = storageService,
        )

    @Nested
    @DisplayName("발송 게이팅 — credential 미충족 시 no-op(EMPTY) + S3 미접근")
    inner class Gating {

        @Test
        @DisplayName("빈 토큰 목록이면 EMPTY (S3 접근 없음)")
        fun emptyTokens() {
            val result = sender(enabled = true, s3Key = "config/fcm/key.json")
                .sendNotificationToTokens(emptyList(), "t", "b", emptyMap())

            assertThat(result).isEqualTo(FcmSendResult.EMPTY)
            verify(exactly = 0) { storageService.download(any()) }
        }

        @Test
        @DisplayName("enabled=false 면 EMPTY + S3 미접근")
        fun disabled() {
            val result = sender(enabled = false, s3Key = "config/fcm/key.json")
                .sendNotificationToTokens(listOf("token-a"), "t", "b", emptyMap())

            assertThat(result).isEqualTo(FcmSendResult.EMPTY)
            verify(exactly = 0) { storageService.download(any()) }
        }

        @Test
        @DisplayName("credential-s3-key blank 면 EMPTY + S3 미접근")
        fun blankS3Key() {
            val result = sender(enabled = true, s3Key = "  ")
                .sendNotificationToTokens(listOf("token-a"), "t", "b", emptyMap())

            assertThat(result).isEqualTo(FcmSendResult.EMPTY)
            verify(exactly = 0) { storageService.download(any()) }
        }

        @Test
        @DisplayName("credential-s3-key null 이면 EMPTY + S3 미접근")
        fun nullS3Key() {
            val result = sender(enabled = true, s3Key = null)
                .sendNotificationToTokens(listOf("token-a"), "t", "b", emptyMap())

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
                .sendNotificationToTokens(listOf("token-a"), "t", "b", emptyMap())

            assertThat(result).isEqualTo(FcmSendResult.EMPTY)
            verify(exactly = 1) { storageService.download("config/fcm/key.json") }
        }

        @Test
        @DisplayName("S3 객체가 유효하지 않은 JSON → credential 파싱 실패해도 graceful skip(EMPTY)")
        fun s3ObjectInvalidJson() {
            every { storageService.download("config/fcm/key.json") } returns
                "not a valid service account json".toByteArray()

            val result = sender(enabled = true, s3Key = "config/fcm/key.json")
                .sendNotificationToTokens(listOf("token-a"), "t", "b", emptyMap())

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

            s.sendNotificationToTokens(listOf("token-a"), "t", "b", emptyMap())
            s.sendNotificationToTokens(listOf("token-b"), "t", "b", emptyMap())

            // resolveMessaging 이 initialized 플래그로 1회만 초기화 → S3 다운로드도 1회
            verify(exactly = 1) { storageService.download("config/fcm/key.json") }
        }
    }
}
