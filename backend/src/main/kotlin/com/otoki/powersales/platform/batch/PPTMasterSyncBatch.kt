package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.domain.activity.promotion.service.PPTMasterBatchService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("dev | prod")
@ConditionalOnProperty(name = ["app.batch.ppt-master.sync.enabled"], havingValue = "true", matchIfMissing = false)
class PPTMasterSyncBatch(
    private val pptMasterBatchService: PPTMasterBatchService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = "0 44 * * * *")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    fun run() {
        scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
            pptMasterBatchService.syncValidMasters(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "pptMaster.syncValid"
    }
}
