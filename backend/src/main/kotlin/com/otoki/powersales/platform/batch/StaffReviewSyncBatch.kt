package com.otoki.powersales.platform.batch

import com.otoki.powersales.domain.org.employee.sfsync.StaffReviewSyncService
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * SF 사원평가 마스터(`StaffReview__c`) 주기 sync 배치.
 *
 * 매일 새벽 03시 SF 변경분(MOD_DT=당일, 수정일 기준)을 fetch 하여 SF 레코드 Id(sfid) 기준 upsert 한다.
 * 분산 환경 중복 실행은 ShedLock 으로 차단하고, 실행 이력은 [ScheduledJobRunner] 가
 * `scheduled_job_run` 에 영속화한다.
 */
@Component
@Profile("!local")
class StaffReviewSyncBatch(
    private val syncService: StaffReviewSyncService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = CRON)
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT55M", lockAtLeastFor = "PT1M")
    fun run() {
        scheduledJobRunner.run(JOB_NAME) { ctx ->
            syncService.sync(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "staffReview.sync"
        // 매일 03시. application.yml override 가능.
        const val CRON = "\${app.batch.staff-review.sync.cron:0 0 3 * * *}"
    }
}
