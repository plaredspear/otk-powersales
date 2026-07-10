package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.external.sap.outbox.SapOutboxBatchService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.sap.outbox.enabled"], havingValue = "true", matchIfMissing = false)
class SapOutboxBatch(
    private val sapOutboxBatchService: SapOutboxBatchService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    private val log = LoggerFactory.getLogger(SapOutboxBatch::class.java)

    @Scheduled(cron = "\${app.sap.outbox.cron:0 */5 * * * *}")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT5M", lockAtLeastFor = "PT10S")
    fun run() {
        try {
            scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
                sapOutboxBatchService.execute(ctx)
            }
        } catch (ex: Exception) {
            log.error("SAP outbox 워커 실행 실패", ex)
        }
    }

    companion object {
        const val JOB_NAME = "sap-outbox-worker"
    }
}
