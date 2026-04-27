package com.otoki.powersales.common.jobrun

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

/**
 * `@Scheduled` 잡 본문을 감싸 실행 이력을 `scheduled_job_run` 테이블에 영속화한다 (스펙 #548).
 *
 * - INSERT (시작 row, status = RUNNING) 와 종료 UPDATE (status = SUCCESS / FAILURE) 는
 *   각각 REQUIRES_NEW 트랜잭션으로 분리되어 본문 람다의 트랜잭션 롤백/커밋과 무관하게 영속된다.
 * - Runner 자체의 INSERT/UPDATE 가 실패하더라도 본문 람다 실행과 결과를 가로막지 않으며
 *   WARN 로그만 남긴다 (4.5 — DB 장애 시 운영 잡이 잡히지 않는 fail-open 정책).
 * - ShedLock 인터셉터의 안쪽에서 호출된다고 가정한다 — 본 클래스는 분산 락을 직접 다루지 않는다.
 */
@Service
class ScheduledJobRunner(
    private val repository: ScheduledJobRunRepository,
    private val objectMapper: ObjectMapper,
    transactionManager: PlatformTransactionManager,
) {
    private val log = LoggerFactory.getLogger(ScheduledJobRunner::class.java)

    private val newTxTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun <T> run(jobName: String, block: (ScheduledJobRunContext) -> T): T {
        val startedAt = Instant.now()
        val context = ScheduledJobRunContext(jobName)
        val runId = insertRunningRow(jobName, startedAt)

        val result: T = try {
            block(context)
        } catch (e: Throwable) {
            recordFailure(runId, jobName, e)
            throw e
        }
        recordSuccess(runId, jobName, context.pendingMetadata())
        return result
    }

    private fun insertRunningRow(jobName: String, startedAt: Instant): Long? = try {
        newTxTemplate.execute {
            val saved = repository.save(
                ScheduledJobRun(
                    jobName = jobName,
                    startedAt = startedAt,
                    status = ScheduledJobRun.STATUS_RUNNING,
                    createdAt = startedAt,
                )
            )
            saved.id
        }
    } catch (e: Exception) {
        log.warn("ScheduledJobRunner INSERT 실패: jobName={}", jobName, e)
        null
    }

    private fun recordSuccess(runId: Long?, jobName: String, metadata: Map<String, Any?>?) {
        if (runId == null) return
        try {
            newTxTemplate.executeWithoutResult {
                val row = repository.findById(runId).orElse(null) ?: return@executeWithoutResult
                row.status = ScheduledJobRun.STATUS_SUCCESS
                row.endedAt = Instant.now()
                row.metadata = serializeMetadata(metadata)
                repository.save(row)
            }
        } catch (e: Exception) {
            log.warn("ScheduledJobRunner UPDATE(SUCCESS) 실패: jobName={}, runId={}", jobName, runId, e)
        }
    }

    private fun recordFailure(runId: Long?, jobName: String, cause: Throwable) {
        if (runId == null) return
        try {
            newTxTemplate.executeWithoutResult {
                val row = repository.findById(runId).orElse(null) ?: return@executeWithoutResult
                row.status = ScheduledJobRun.STATUS_FAILURE
                row.endedAt = Instant.now()
                row.errorMessage = formatError(cause)
                repository.save(row)
            }
        } catch (e: Exception) {
            log.warn("ScheduledJobRunner UPDATE(FAILURE) 실패: jobName={}, runId={}", jobName, runId, e)
        }
    }

    private fun serializeMetadata(metadata: Map<String, Any?>?): String? {
        if (metadata == null) return null
        return try {
            objectMapper.writeValueAsString(metadata)
        } catch (e: Exception) {
            log.warn("ScheduledJobRunner metadata 직렬화 실패 - null 로 기록", e)
            null
        }
    }

    private fun formatError(cause: Throwable): String {
        val raw = "${cause::class.simpleName ?: cause::class.java.name}: ${cause.message ?: ""}"
        return if (raw.length <= ScheduledJobRun.ERROR_MESSAGE_MAX_LENGTH) {
            raw
        } else {
            raw.substring(0, ScheduledJobRun.ERROR_MESSAGE_MAX_LENGTH)
        }
    }

    companion object {
        const val RETENTION_DAYS = 90L
    }
}
