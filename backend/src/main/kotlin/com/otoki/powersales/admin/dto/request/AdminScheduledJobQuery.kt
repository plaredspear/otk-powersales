package com.otoki.powersales.admin.dto.request

import java.time.LocalDateTime

/**
 * Admin 스케줄 잡 실행 이력 검색 쿼리 파라미터.
 * `LocalDateTime` 은 ISO-8601 (예: `2026-05-18T00:00:00`) 로 파싱된다 — UTC wall clock 기준.
 */
data class AdminScheduledJobQuery(
    val jobName: String? = null,
    val status: String? = null,
    val from: LocalDateTime? = null,
    val to: LocalDateTime? = null,
    val page: Int = 1,
    val size: Int = 20,
)
