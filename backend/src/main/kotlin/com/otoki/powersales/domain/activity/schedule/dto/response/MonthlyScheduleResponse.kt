package com.otoki.powersales.domain.activity.schedule.dto.response

/**
 * 월간 일정 조회 응답 DTO
 */
data class MonthlyScheduleResponse(
    val year: Int,
    val month: Int,
    val workDays: List<WorkDayDto>,
    val annualLeaveCount: Int,
    val substituteHolidayCount: Int
)

/**
 * 일별 근무 여부 DTO
 *
 * completedCount/totalCount 는 레거시 mgnSchedule 캘린더 셀의 `보고완료 / 총건` 숫자쌍 정합.
 * (레거시 personal calSchedule: sum = commutelogid 있는 거래처 수, cnt = accList 거래처 수)
 */
data class WorkDayDto(
    val date: String, // YYYY-MM-DD 형식
    val hasWork: Boolean,
    val workingType: String? = null,
    val completedCount: Int = 0, // 보고완료 거래처 수 (레거시 sum)
    val totalCount: Int = 0 // 담당 거래처 수 (레거시 cnt)
)
