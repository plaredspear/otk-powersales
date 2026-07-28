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

    // 매일 00:50 KST — 레거시 SF CronTrigger "발령정보 스케줄" (`00 50 0 * * ? *`, Asia/Seoul,
    // 2026-07-28 운영 CronTrigger 전수 조회 실측) 정합.
    // JVM/컨테이너 TZ=Asia/Seoul (Dockerfile) 이므로 zone 명시 없이 KST 로 발화.
    @Scheduled(cron = "0 50 0 * * *")
    @SchedulerLock(name = JOB_NAME, lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    fun run() {
        scheduledJobRunner.runScheduled(JOB_NAME) { ctx ->
            postponedAppointmentBatchService.process(ctx)
        }
    }

    companion object {
        const val JOB_NAME = "sap.processPostponedAppointments"
    }
}
