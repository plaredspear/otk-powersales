package com.otoki.powersales.domain.activity.productexpiration.service

import com.otoki.powersales.domain.activity.productexpiration.repository.ProductExpirationRepository
import com.otoki.powersales.platform.push.sender.FcmSendResult
import com.otoki.powersales.platform.push.sender.FcmSender
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("ProductExpirationAlertService - 유통기한 만료 FCM 알림 (레거시 OttogiSalesSchedule.alarm 동등)")
class ProductExpirationAlertServiceTest {

    private val productExpirationRepository: ProductExpirationRepository = mockk()
    private val fcmSender: FcmSender = mockk()
    private val service = ProductExpirationAlertService(productExpirationRepository, fcmSender)

    private val today = LocalDate.of(2026, 6, 18)

    @Test
    @DisplayName("alarm_date 당일 토큰을 모아 레거시 문구로 발송하고 집계를 반환한다")
    fun sendsToTokensWithLegacyMessage() {
        every { productExpirationRepository.findDistinctFcmTokensByAlarmDate(today) } returns
            listOf("token-a", "token-b", "token-c")
        every { fcmSender.sendNotificationToTokens(any(), any(), any()) } returns
            FcmSendResult(successCount = 2, failureCount = 1)

        val result = service.sendDailyAlerts(today)

        verify(exactly = 1) {
            fcmSender.sendNotificationToTokens(
                listOf("token-a", "token-b", "token-c"),
                "유통기한  임박",
                "오늘 유통기한 임박제품이 있습니다.",
            )
        }
        assertThat(result.targetTokens).isEqualTo(3)
        assertThat(result.successCount).isEqualTo(2)
        assertThat(result.failureCount).isEqualTo(1)
    }

    @Test
    @DisplayName("대상 토큰이 없으면 발송하지 않고 0 집계를 반환한다")
    fun noopWhenNoTokens() {
        every { productExpirationRepository.findDistinctFcmTokensByAlarmDate(today) } returns emptyList()

        val result = service.sendDailyAlerts(today)

        verify(exactly = 0) { fcmSender.sendNotificationToTokens(any(), any(), any()) }
        assertThat(result.targetTokens).isEqualTo(0)
        assertThat(result.successCount).isEqualTo(0)
        assertThat(result.failureCount).isEqualTo(0)
    }
}
