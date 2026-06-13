package com.otoki.powersales.platform.batch

import com.otoki.powersales.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.promotion.service.PPTMasterBatchService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("!local")
class PPTMasterSyncBatch(
    private val pptMasterBatchService: PPTMasterBatchService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = "0 0 5 * * *")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    fun run() {
        scheduledJobRunner.run(JOB_NAME) { ctx ->
            pptMasterBatchService.syncValidMasters(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "pptMaster.syncValid"
    }
}
