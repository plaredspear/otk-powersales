package com.otoki.powersales.admin.dto.request

import java.time.LocalDateTime

/**
 * Admin 외부 API 호출 이력 검색 쿼리 파라미터.
 *
 * `LocalDateTime` 은 ISO-8601 (예: `2026-06-16T00:00:00`) 로 파싱된다.
 * [endpointKey] 는 web 외부 API 테스트 탭 key (예: `loan-inquiry`, `staff-review-sync`) — 탭별 이력 필터.
 */
data class AdminExternalApiLogQuery(
    val targetSystem: String? = null,
    val endpointKey: String? = null,
    val success: Boolean? = null,
    val httpMethod: String? = null,
    val from: LocalDateTime? = null,
    val to: LocalDateTime? = null,
    val page: Int = 1,
    val size: Int = 20,
)
