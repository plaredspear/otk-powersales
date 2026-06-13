package com.otoki.powersales.platform.batch

import com.otoki.powersales.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.schedule.service.DisplayMasterBatchService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DisplayMasterSapOutboundBatch(
    private val displayMasterBatchService: DisplayMasterBatchService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = "\${app.sap.outbound.display.cron:0 0 1 * * *}")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    fun run() {
        scheduledJobRunner.run(JOB_NAME) { ctx ->
            displayMasterBatchService.runDaily(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "display-master-sap-batch"
    }
}
