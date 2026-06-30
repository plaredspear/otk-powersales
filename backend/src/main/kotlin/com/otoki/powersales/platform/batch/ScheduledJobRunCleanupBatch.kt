package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunCleanupService
import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("dev | prod")
@ConditionalOnProperty(name = ["app.batch.scheduled-job-run-cleanup.enabled"], havingValue = "true", matchIfMissing = false)
class ScheduledJobRunCleanupBatch(
    private val cleanupService: ScheduledJobRunCleanupService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    private val log = LoggerFactory.getLogger(ScheduledJobRunCleanupBatch::class.java)

    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT5M", lockAtLeastFor = "PT1M")
    fun run() {
        try {
            scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
                cleanupService.cleanup(ctx)
            }
        } catch (e: Exception) {
            log.error("ScheduledJobRunCleanup 실패", e)
        }
    }

    companion object {
        const val JOB_NAME = "scheduledJobRun.cleanup"
    }
}
