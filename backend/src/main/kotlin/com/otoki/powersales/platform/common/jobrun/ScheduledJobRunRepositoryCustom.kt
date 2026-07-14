package com.otoki.powersales.platform.common.jobrun

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface ScheduledJobRunRepositoryCustom {
    /**
     * `started_at < threshold` 인 row 를 단일 bulk DELETE 로 삭제하고 영향받은 row 수를 반환한다.
     * 보존 정책 잡(`ScheduledJobRunCleanupBatch`) 에서 매일 04:00 호출된다.
     */
    fun deleteByStartedAtBefore(threshold: LocalDateTime): Long

    /**
     * 운영자 admin 화면용 이력 검색. 필터는 모두 optional.
     * - `jobName` 일치 (정확 매칭)
     * - `status` 일치 (RUNNING / SUCCESS / FAILURE)
     * - `startedAt >= from`, `startedAt < to` 의 반-개구간
     *
     * 정렬은 `started_at DESC`. 운영 화면 페이지네이션 용.
     */
    fun search(
        jobName: String?,
        status: String?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        pageable: Pageable,
    ): Page<ScheduledJobRun>

    /**
     * `started_at IN [from, to)` 범위 내 status 별 row 수.
     * 요약 위젯 (24h 카운트) 용 단일 group-by 쿼리.
     */
    fun countByStatusWithin(from: LocalDateTime, to: LocalDateTime): Map<String, Long>

    /**
     * 테이블에 실제 등장한 distinct jobName 목록 (가나다순).
     * UI 필터 드롭다운에서 [com.otoki.powersales.platform.batch.ScheduledJobCatalog] 와 union 하여 사용한다.
     */
    fun findDistinctJobNames(): List<String>

    /**
     * `started_at IN [from, to)` 범위 내 잡별 실행 집계.
     *
     * 대시보드 일별 실행현황 위젯 용 — 잡마다 (전체/성공/실패/스킵/실행중 수 + 마지막 실행 시각·상태) 를
     * 단일 조회로 산출한다. [jobNames] 로 대상 잡을 제한하며, 이력이 0건인 잡은 결과에 포함되지 않는다
     * (호출부에서 미실행으로 처리).
     */
    fun aggregateByJobNameWithin(
        jobNames: Collection<String>,
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<JobRunAggregate>
}

/**
 * 잡 1개의 윈도우 내 실행 집계.
 *
 * @property jobName 잡 이름
 * @property totalCount 윈도우 내 전체 실행 row 수 (모든 status 합)
 * @property successCount SUCCESS 수
 * @property failureCount FAILURE 수
 * @property skippedCount SKIPPED 수 (런타임 토글 OFF 로 발화했으나 본문 미실행)
 * @property runningCount RUNNING 수 (미종료)
 * @property lastStartedAt 윈도우 내 가장 최근 실행 시작 시각 (없으면 null — 호출부에서 미실행)
 * @property lastStatus 가장 최근 실행의 status
 */
data class JobRunAggregate(
    val jobName: String,
    val totalCount: Long,
    val successCount: Long,
    val failureCount: Long,
    val skippedCount: Long,
    val runningCount: Long,
    val lastStartedAt: LocalDateTime?,
    val lastStatus: String?,
)
