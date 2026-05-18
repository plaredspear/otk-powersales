package com.otoki.powersales.admin.dto.request

import java.time.LocalDateTime

/**
 * Admin SAP 아웃바운드 호출 이력 검색 쿼리 파라미터.
 *
 * `LocalDateTime` 은 ISO-8601 (예: `2026-05-18T00:00:00`) 로 파싱된다.
 */
data class AdminSapOutboundLogQuery(
    val interfaceId: String? = null,
    val resultCode: String? = null,
    val from: LocalDateTime? = null,
    val to: LocalDateTime? = null,
    val page: Int = 1,
    val size: Int = 20,
)
