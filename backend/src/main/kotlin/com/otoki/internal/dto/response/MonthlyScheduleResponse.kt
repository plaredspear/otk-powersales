package com.otoki.internal.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 월간 일정 조회 응답 DTO
 */
data class MonthlyScheduleResponse(
    val year: Int,
    val month: Int,
    @JsonProperty("work_days")
    val workDays: List<WorkDayDto>
)

/**
 * 일별 근무 여부 DTO
 */
data class WorkDayDto(
    val date: String, // YYYY-MM-DD 형식
    @JsonProperty("has_work")
    val hasWork: Boolean
)
