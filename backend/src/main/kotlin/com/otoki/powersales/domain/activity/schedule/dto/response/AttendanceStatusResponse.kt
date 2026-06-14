package com.otoki.powersales.domain.activity.schedule.dto.response

/**
 * 출근 현황 응답 DTO
 */
data class AttendanceStatusResponse(
    val totalCount: Int,
    val registeredCount: Int,
    val statusList: List<AttendanceStatusItem>,
    val currentDate: String
)

/**
 * 출근 현황 항목
 */
data class AttendanceStatusItem(
    val scheduleId: Long,
    val accountName: String,
    val workCategory: String,
    val status: String
)
