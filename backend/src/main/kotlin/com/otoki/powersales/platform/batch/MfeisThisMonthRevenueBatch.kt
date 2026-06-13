package com.otoki.powersales.platform.batch

import com.otoki.powersales.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.schedule.service.MfeisThisMonthRevenueBatchService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * MFEIS `this_month_amount` 월간 일괄 갱신 batch 진입 클래스 (spec #680 §5.2).
 *
 * legacy `UpdateThisMonthRevenueBatch.cls` (Schedulable + Batchable) 동등 복원. cron 사용자 결정
 * (#680 Q15 옵션 1): `0 0 3 1 * ?` 매월 1일 03:00 (전월 마감 + 일 경과 후 안정 시점).
 * ShedLock (#680 Q13 옵션 1): chunk 200 + `lockAtMostFor=PT30M / lockAtLeastFor=PT5M`.
 * 처리 로직은 [MfeisThisMonthRevenueBatchService.runMonthly] 위임.
 *
 * 현재 [BatchConfig] 의 `@EnableScheduling` 전면 임시 비활성 (commit d927d1bc, 2026-05-14) — 본 batch
 * fire 도 비활성 상태와 무관. 어노테이션 한 줄 복원 시 즉시 활성.
 */
@Component
class MfeisThisMonthRevenueBatch(
    private val batchService: MfeisThisMonthRevenueBatchService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = "\${app.batch.mfeis.this-month-revenue.cron:0 0 3 1 * ?}")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    fun run() {
        scheduledJobRunner.run(JOB_NAME) { ctx ->
            batchService.runMonthly(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "mfeis-this-month-revenue-batch"
    }
}
