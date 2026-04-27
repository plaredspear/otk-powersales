package com.otoki.powersales.common.jobrun

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * `scheduled_job_run` 테이블의 보존 정책 잡 (스펙 #548 §4.6).
 *
 * - 매일 04:00 에 90일 초과 row 를 단일 bulk DELETE 로 정리.
 * - ShedLock 으로 다중 인스턴스 동시 실행 방지, ScheduledJobRunner 로 자기 실행 이력 기록.
 * - `@Profile("!local")` — local 프로파일에서는 미동작 (다른 잡과 동일 정책).
 */
@Component
@Profile("!local")
class ScheduledJobRunCleanupJob(
    private val repository: ScheduledJobRunRepository,
    private val runner: ScheduledJobRunner,
) {
    private val log = LoggerFactory.getLogger(ScheduledJobRunCleanupJob::class.java)

    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(
        name = "scheduledJobRun.cleanup",
        lockAtMostFor = "PT5M",
        lockAtLeastFor = "PT1M"
    )
    fun cleanup() {
        log.info("ScheduledJobRunCleanup 시작")
        try {
            runner.run("scheduledJobRun.cleanup") { context ->
                val threshold = Instant.now().minus(ScheduledJobRunner.RETENTION_DAYS, ChronoUnit.DAYS)
                val deleted = repository.deleteByStartedAtBefore(threshold)
                context.metadata(mapOf("deleted" to deleted))
                log.info("ScheduledJobRunCleanup 완료: deleted={}", deleted)
            }
        } catch (e: Exception) {
            log.error("ScheduledJobRunCleanup 실패", e)
        }
    }
}
