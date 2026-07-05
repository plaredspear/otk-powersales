package com.otoki.powersales.platform.batch

import com.otoki.powersales.domain.activity.productexpiration.service.ProductExpirationAlertService
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
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
 * 전역 `@EnableScheduling` (BatchConfig) 은 ON 이며, 잡별 게이팅으로 제어한다.
 * `app.batch.product-expiration-alert.enabled=true` 인 환경에서만 빈이 생성·발화한다 (기본 OFF —
 * dev/prod `application.yml` 에서 활성화됨). 단, 실제 FCM 발송은 `app.push.fcm.enabled` + credential 주입 시에만 이루어진다.
 */
@Component
@Profile("dev | prod")
@ConditionalOnProperty(name = ["app.batch.product-expiration-alert.enabled"], havingValue = "true", matchIfMissing = false)
class ProductExpirationAlertBatch(
    private val productExpirationAlertService: ProductExpirationAlertService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = "\${app.batch.product-expiration-alert.cron:0 0 0 * * *}")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    fun run() {
        scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
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
