package com.otoki.powersales.domain.activity.productexpiration.service

import com.otoki.powersales.domain.activity.productexpiration.repository.ProductExpirationRepository
import com.otoki.powersales.platform.push.dto.PushTargetEmployee
import com.otoki.powersales.platform.push.sender.FcmSendResult
import com.otoki.powersales.platform.push.sender.FcmSender
import com.otoki.powersales.platform.push.sender.PushTarget
import com.otoki.powersales.platform.push.service.PushBadgeService
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
    private val pushBadgeService: PushBadgeService = mockk()
    private val service = ProductExpirationAlertService(
        productExpirationRepository,
        fcmSender,
        pushBadgeService,
    )

    private val today = LocalDate.of(2026, 6, 18)

    @Test
    @DisplayName("alarm_date 당일 대상을 모아 레거시 문구 + 배지 절대값으로 발송하고 집계를 반환한다")
    fun sendsToTargetsWithLegacyMessageAndBadge() {
        val targets = listOf(
            PushTargetEmployee(1L, "token-a"),
            PushTargetEmployee(2L, "token-b"),
            PushTargetEmployee(3L, "token-c"),
        )
        val withBadge = listOf(
            PushTarget("token-a", 1),
            PushTarget("token-b", 5),
            PushTarget("token-c", 2),
        )
        every { productExpirationRepository.findDistinctPushTargetsByAlarmDate(today) } returns targets
        every { pushBadgeService.increaseAndBuildTargets(targets) } returns withBadge
        every { fcmSender.sendNotification(any(), any(), any()) } returns
            FcmSendResult(successCount = 2, failureCount = 1)

        val result = service.sendDailyAlerts(today)

        // 배지 카운터를 먼저 +1 한 뒤 그 절대값이 실린 대상으로 발송해야 한다.
        verify(exactly = 1) { pushBadgeService.increaseAndBuildTargets(targets) }
        verify(exactly = 1) {
            fcmSender.sendNotification(
                withBadge,
                "유통기한  임박",
                "오늘 유통기한 임박제품이 있습니다.",
            )
        }
        assertThat(result.targetTokens).isEqualTo(3)
        assertThat(result.successCount).isEqualTo(2)
        assertThat(result.failureCount).isEqualTo(1)
    }

    @Test
    @DisplayName("대상이 없으면 배지 증가/발송 없이 0 집계를 반환한다")
    fun noopWhenNoTargets() {
        every { productExpirationRepository.findDistinctPushTargetsByAlarmDate(today) } returns emptyList()

        val result = service.sendDailyAlerts(today)

        verify(exactly = 0) { pushBadgeService.increaseAndBuildTargets(any()) }
        verify(exactly = 0) { fcmSender.sendNotification(any(), any(), any()) }
        assertThat(result.targetTokens).isEqualTo(0)
        assertThat(result.successCount).isEqualTo(0)
        assertThat(result.failureCount).isEqualTo(0)
    }
}
