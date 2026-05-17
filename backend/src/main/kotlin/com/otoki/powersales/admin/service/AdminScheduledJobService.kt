package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.request.AdminScheduledJobQuery
import com.otoki.powersales.admin.dto.response.RegisteredScheduledJobDto
import com.otoki.powersales.admin.dto.response.ScheduledJobRunDto
import com.otoki.powersales.admin.dto.response.ScheduledJobRunListResponse
import com.otoki.powersales.admin.dto.response.ScheduledJobSummaryResponse
import com.otoki.powersales.batch.ScheduledJobCatalog
import com.otoki.powersales.common.jobrun.ScheduledJobRun
import com.otoki.powersales.common.jobrun.ScheduledJobRunRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 운영자 admin 화면용 `@Scheduled` 잡 실행 이력 조회 서비스.
 *
 * 시각 필드는 모두 UTC wall clock `LocalDateTime` 으로 처리한다 (전사 컨벤션 — 스펙 #564).
 * 요약 위젯의 `windowFrom`/`windowTo` 는 서버 `LocalDateTime.now()` 기준이며 응답에 그대로 포함되어
 * 화면이 사용자에게 윈도우 시각을 표기할 수 있다.
 */
@Service
@Transactional(readOnly = true)
class AdminScheduledJobService(
    private val scheduledJobRunRepository: ScheduledJobRunRepository,
) {

    fun search(query: AdminScheduledJobQuery): ScheduledJobRunListResponse {
        val page = (query.page - 1).coerceAtLeast(0)
        val size = query.size.coerceIn(1, MAX_PAGE_SIZE)
        val pageable = PageRequest.of(page, size)

        val result = scheduledJobRunRepository.search(
            jobName = query.jobName,
            status = query.status,
            from = query.from,
            to = query.to,
            pageable = pageable,
        )

        return ScheduledJobRunListResponse(
            items = result.content.map { it.toDto() },
            totalCount = result.totalElements,
            currentPage = query.page,
            pageSize = size,
        )
    }

    fun summary(windowHours: Long): ScheduledJobSummaryResponse {
        val to = LocalDateTime.now()
        val from = to.minusHours(windowHours.coerceIn(1L, MAX_WINDOW_HOURS))
        val counts = scheduledJobRunRepository.countByStatusWithin(from, to)
        val running = counts[ScheduledJobRun.STATUS_RUNNING] ?: 0L
        val success = counts[ScheduledJobRun.STATUS_SUCCESS] ?: 0L
        val failure = counts[ScheduledJobRun.STATUS_FAILURE] ?: 0L

        val distinctFromTable = scheduledJobRunRepository.findDistinctJobNames()
        val distinctMerged = (ScheduledJobCatalog.JOB_NAMES + distinctFromTable).distinct().sorted()

        return ScheduledJobSummaryResponse(
            windowFrom = from,
            windowTo = to,
            totalCount = running + success + failure,
            runningCount = running,
            successCount = success,
            failureCount = failure,
            distinctJobNames = distinctMerged,
        )
    }

    fun catalog(): List<RegisteredScheduledJobDto> =
        ScheduledJobCatalog.ENTRIES.map {
            RegisteredScheduledJobDto(
                jobName = it.jobName,
                cron = it.cron,
                description = it.description,
            )
        }

    private fun ScheduledJobRun.toDto(): ScheduledJobRunDto {
        val durationMs = endedAt?.let { end ->
            java.time.Duration.between(startedAt, end).toMillis()
        }
        return ScheduledJobRunDto(
            id = id,
            jobName = jobName,
            startedAt = startedAt,
            endedAt = endedAt,
            durationMs = durationMs,
            status = status,
            errorMessage = errorMessage,
            metadata = metadata,
        )
    }

    companion object {
        const val MAX_PAGE_SIZE = 100
        const val MAX_WINDOW_HOURS = 24L * 90
    }
}
