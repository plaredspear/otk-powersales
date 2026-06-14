package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.domain.sales.sfsync.SalesProgressRateMasterSyncService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * SF 거래처목표등록마스터(`SalesProgressRateMaster__c`) 주기 sync 배치.
 *
 * 1시간마다 SF 변경분을 fetch 하여 ExternalKey 기준 upsert 한다. 분산 환경 중복 실행은
 * ShedLock 으로 차단하고, 실행 이력은 [ScheduledJobRunner] 가 `scheduled_job_run` 에 영속화한다.
 */
@Component
@Profile("!local")
class SalesProgressRateMasterSyncBatch(
    private val syncService: SalesProgressRateMasterSyncService,
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
        const val JOB_NAME = "salesProgressRateMaster.sync"
        // 매시 정각 (1시간 주기). application.yml override 가능.
        const val CRON = "\${app.batch.sales-progress-rate-master.sync.cron:0 0 * * * *}"
    }
}
