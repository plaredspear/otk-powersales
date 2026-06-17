package com.otoki.powersales.platform.batch

import com.otoki.powersales.domain.activity.productexpiration.service.ProductExpirationAlertService
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 유통기한 만료 알림(FCM 푸시) 배치 (매일).
 *
 * 레거시 `OttogiSalesSchedule.alarm()` (Spring `@Scheduled`, 매일 00:00 — 운영 cron) 동등.
 * `alarm_date` 가 당일인 유통기한 레코드의 담당 여사원에게 FCM 푸시를 발송한다.
 * 기본 cron 은 매일 00:00 (`app.batch.product-expiration-alert.cron` 으로 재정의 가능).
 *
 * ⚠️ 전역 `@EnableScheduling` (BatchConfig) 활성 시에만 실제 발화한다 (현재 비활성).
 */
@Component
@Profile("!local")
class ProductExpirationAlertBatch(
    private val productExpirationAlertService: ProductExpirationAlertService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = "\${app.batch.product-expiration-alert.cron:0 0 0 * * *}")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    fun run() {
        scheduledJobRunner.run(JOB_NAME) { ctx ->
            val result = productExpirationAlertService.sendDailyAlerts(LocalDate.now())
            ctx.metadata(
                mapOf(
                    "targetTokens" to result.targetTokens,
                    "successCount" to result.successCount,
                    "failureCount" to result.failureCount,
                )
            )
        }
    }

    companion object {
        const val JOB_NAME = "productExpiration.alarm"
    }
}
