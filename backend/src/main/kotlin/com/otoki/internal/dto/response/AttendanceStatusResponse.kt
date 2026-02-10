package com.otoki.internal.dto.response

import java.time.LocalDateTime

/**
 * 출근등록 현황 응답 DTO
 */
data class AttendanceStatusResponse(
    val totalCount: Int,
    val registeredCount: Int,
    val statusList: List<AttendanceStatusInfo>,
    val currentDate: String
)

/**
 * 거래처별 출근등록 현황
 */
data class AttendanceStatusInfo(
    val storeId: Long,
    val storeName: String,
    val status: String,
    val workType: String?,
    val registeredAt: LocalDateTime?
)
