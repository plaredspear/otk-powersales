package com.otoki.powersales.batch

import com.otoki.powersales.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.sales.materialize.OroraSalesMaterializeFacade
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * ORORA 월별 마감 → `monthly_sales_history` 적재 daily batch (Spec #855).
 *
 * legacy `IF_REST_ORORA_ReceiveMonthlySalesHistory` (ORORA pull, ABC/Ship 마감) 동등.
 * SAP PO 어댑터 callout → ORORA DB 직조회. 대상 월은 당월 동적 산출 (Q2). 처리 로직은
 * [OroraSalesMaterializeFacade.materializeMonthly] 위임.
 *
 * 현재 [BatchConfig] 의 `@EnableScheduling` 전면 임시 비활성 (commit d927d1bc, 2026-05-14) — 본 batch
 * fire 도 비활성 상태와 무관. 어노테이션 한 줄 복원 시 즉시 활성.
 */
@Component
class OroraMonthlySalesMaterializeBatch(
    private val facade: OroraSalesMaterializeFacade,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = "\${app.batch.orora.monthly.cron:0 0 5 * * *}")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT30M", lockAtLeastFor = "PT30S")
    fun run() {
        scheduledJobRunner.run(JOB_NAME) { ctx ->
            val result = facade.materializeMonthly()
            ctx.metadata(
                mapOf(
                    "trigger" to "scheduled",
                    "salesMonth" to result.salesMonth,
                    "fetchedCount" to result.fetchedCount,
                    "upsertedCount" to result.upsertedCount,
                    "skippedAccountUnmatchedCount" to result.skippedAccountUnmatchedCount,
                )
            )
        }
    }

    companion object {
        const val JOB_NAME = "orora-monthly-sales-materialize-batch"
    }
}
