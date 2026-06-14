package com.otoki.powersales.domain.activity.schedule.dto.response

/**
 * 일간 일정 상세 조회 응답 DTO
 */
data class DailyScheduleResponse(
    val date: String, // YYYY-MM-DD 형식
    val dayOfWeek: String,
    val memberName: String,
    val employeeCode: String?,
    val workingType: String? = null,
    val reportProgress: ReportProgressDto,
    val accounts: List<DisplayWorkScheduleItemDto>
)

/**
 * 보고 진행 상황 DTO
 */
data class ReportProgressDto(
    val completed: Int,
    val total: Int,
    val workType: String
)

/**
 * 거래처 일정 항목 DTO
 */
data class DisplayWorkScheduleItemDto(
    val accountId: Long,
    val accountName: String,
    val workType1: String,
    val workType2: String,
    val workType3: String,
    val isRegistered: Boolean
)
