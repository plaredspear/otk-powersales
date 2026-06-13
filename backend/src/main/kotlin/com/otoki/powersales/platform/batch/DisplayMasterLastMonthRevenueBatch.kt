package com.otoki.powersales.platform.batch

import com.otoki.powersales.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.schedule.service.DisplayMasterLastMonthRevenueBatchService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * DisplayWorkSchedule.lastMonthRevenue daily batch 진입 클래스 (spec #690).
 *
 * legacy `UpdateLastMonthRevenueBatch.cls` (Schedulable + Batchable) 동등 복원. cron 사용자 결정 daily
 * `0 0 2 * * *` (#670 §3 Q3 인계 — 매출 적재 시점 의존성 회피). 처리 로직은
 * [DisplayMasterLastMonthRevenueBatchService.runDaily] 위임.
 *
 * 현재 [BatchConfig] 의 `@EnableScheduling` 전면 임시 비활성 (commit d927d1bc, 2026-05-14) — 본 batch
 * fire 도 비활성 상태와 무관. 어노테이션 한 줄 복원 시 즉시 활성.
 */
@Component
class DisplayMasterLastMonthRevenueBatch(
    private val batchService: DisplayMasterLastMonthRevenueBatchService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = "\${app.batch.display.last-month-revenue.cron:0 0 2 * * *}")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT15M", lockAtLeastFor = "PT30S")
    fun run() {
        scheduledJobRunner.run(JOB_NAME) { ctx ->
            batchService.runDaily(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "display-master-last-month-revenue-batch"
    }
}
