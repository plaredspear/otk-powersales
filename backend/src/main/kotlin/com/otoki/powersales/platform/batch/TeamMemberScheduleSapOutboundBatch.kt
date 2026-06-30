package com.otoki.powersales.platform.batch

import com.otoki.powersales.platform.common.jobrun.ScheduledJobRunner
import com.otoki.powersales.domain.activity.schedule.service.TeamMemberScheduleSapBatchService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["app.sap.outbound.team-member-schedule.enabled"], havingValue = "true", matchIfMissing = false)
class TeamMemberScheduleSapOutboundBatch(
    private val teamMemberScheduleSapBatchService: TeamMemberScheduleSapBatchService,
    private val scheduledJobRunner: ScheduledJobRunner,
) {

    @Scheduled(cron = "\${app.sap.outbound.team-member-schedule.cron:0 0 1 * * *}")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT10M", lockAtLeastFor = "PT30S")
    fun run() {
        scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
            teamMemberScheduleSapBatchService.runDaily(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "attendance-sap-batch"
    }
}
