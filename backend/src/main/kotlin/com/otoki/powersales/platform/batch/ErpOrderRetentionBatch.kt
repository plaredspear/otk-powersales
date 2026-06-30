package com.otoki.powersales.platform.batch

import com.otoki.powersales.domain.activity.order.service.ErpOrderRetentionService
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 보관주기(6개월) 경과 ERP 주문/라인 정리 배치 (주 1회).
 *
 * 레거시 `Batch_ERPOrderDel` + `Batch_ERPOrderProductDel` (`OrderDate__c < LAST_N_MONTHS:6`, Schedulable) 동등.
 * 기본 cron 은 매주 일요일 04:00 (`app.batch.erp-order-retention.cron` 으로 재정의 가능).
 *
 * ⚠️ 전역 `@EnableScheduling` (BatchConfig) 활성 시에만 실제 발화한다.
 * `app.batch.erp-order-retention.enabled=true` 인 환경에서만 빈이 생성·발화한다 (기본 OFF).
 */
@Component
@Profile("dev | prod")
@ConditionalOnProperty(name = ["app.batch.erp-order-retention.enabled"], havingValue = "true", matchIfMissing = false)
class ErpOrderRetentionBatch(
    private val erpOrderRetentionService: ErpOrderRetentionService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = "\${app.batch.erp-order-retention.cron:0 0 4 * * SUN}")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    fun run() {
        scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
            val result = erpOrderRetentionService.purgeExpired()
            ctx.metadata(
                mapOf(
                    "cutoff" to result.cutoff.toString(),
                    "deletedOrders" to result.deletedOrders,
                    "deletedLines" to result.deletedLines,
                )
            )
        }
    }

    companion object {
        const val JOB_NAME = "erpOrder.retention"
    }
}
