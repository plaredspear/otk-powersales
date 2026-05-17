package com.otoki.powersales.common.jobrun

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
     * UI 필터 드롭다운에서 [com.otoki.powersales.batch.ScheduledJobCatalog] 와 union 하여 사용한다.
     */
    fun findDistinctJobNames(): List<String>
}
