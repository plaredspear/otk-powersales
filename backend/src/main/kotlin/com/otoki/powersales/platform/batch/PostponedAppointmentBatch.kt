package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.external.sap.inbound.service.PostponedAppointmentBatchService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.batch.postponed-appointment.enabled"], havingValue = "true", matchIfMissing = false)
class PostponedAppointmentBatch(
    private val postponedAppointmentBatchService: PostponedAppointmentBatchService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    fun run() {
        scheduledJobRunner.run(JOB_NAME) { ctx ->
            postponedAppointmentBatchService.process(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "sap.processPostponedAppointments"
    }
}
