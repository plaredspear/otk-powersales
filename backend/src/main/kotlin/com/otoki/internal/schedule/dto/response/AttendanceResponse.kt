package com.otoki.internal.schedule.dto.response

/**
 * 출근 등록 응답 DTO
 */
data class CommuteResponse(
    val scheduleId: Long,
    val accountName: String,
    val workType: String?,
    val distanceKm: Double,
    val totalCount: Int,
    val registeredCount: Int
)
