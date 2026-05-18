package com.otoki.powersales.admin.dto.request

import java.time.LocalDateTime

/**
 * Admin SAP 인바운드 audit 검색 쿼리 파라미터.
 *
 * `LocalDateTime` 은 ISO-8601 (예: `2026-05-18T00:00:00`) 로 파싱된다.
 */
data class AdminSapInboundAuditQuery(
    val clientId: String? = null,
    val eventType: String? = null,
    val endpoint: String? = null,
    val from: LocalDateTime? = null,
    val to: LocalDateTime? = null,
    val page: Int = 1,
    val size: Int = 20,
)
