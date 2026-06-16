package com.otoki.powersales.admin.dto.response

import java.time.LocalDateTime

/**
 * 외부 API 호출 이력 응답 DTO (개발자 도구 — 외부 API 테스트 > 호출 이력).
 *
 * `external_api_log` 테이블을 노출한다. [endpointKey] 로 web 탭별 호출 이력을 구분한다.
 */
data class ExternalApiLogRow(
    val id: Long,
    val targetSystem: String,
    val endpointKey: String?,
    val httpMethod: String,
    val uri: String,
    val httpStatus: Int?,
    val success: Boolean,
    val durationMs: Long,
    val requestedAt: LocalDateTime,
    val completedAt: LocalDateTime,
)

data class ExternalApiLogDetail(
    val id: Long,
    val targetSystem: String,
    val endpointKey: String?,
    val httpMethod: String,
    val uri: String,
    val httpStatus: Int?,
    val success: Boolean,
    val durationMs: Long,
    val errorDetail: String?,
    val requestedAt: LocalDateTime,
    val completedAt: LocalDateTime,
)

data class ExternalApiLogListResponse(
    val items: List<ExternalApiLogRow>,
    val totalCount: Long,
    val currentPage: Int,
    val pageSize: Int,
)
