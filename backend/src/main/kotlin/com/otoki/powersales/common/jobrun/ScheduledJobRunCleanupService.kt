package com.otoki.powersales.common.jobrun

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * `scheduled_job_run` 테이블 보존 정책 처리 (스펙 #548 §4.6). batch 진입점은 [com.otoki.powersales.batch.ScheduledJobRunCleanupBatch].
 *
 * - 90일 초과 row 를 단일 bulk DELETE 로 정리.
 */
@Service
class ScheduledJobRunCleanupService(
    private val repository: ScheduledJobRunRepository,
) {
    private val log = LoggerFactory.getLogger(ScheduledJobRunCleanupService::class.java)

    fun cleanup(context: ScheduledJobRunContext? = null) {
        val threshold = LocalDateTime.now().minus(ScheduledJobRunner.RETENTION_DAYS, ChronoUnit.DAYS)
        val deleted = repository.deleteByStartedAtBefore(threshold)
        log.info("ScheduledJobRunCleanup 완료: deleted={}", deleted)
        context?.metadata(mapOf("deleted" to deleted))
    }
}
