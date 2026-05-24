package com.otoki.powersales.schedule.dto.response

/**
 * 여사원 일정 캘린더 — 일정 이벤트 1건.
 *
 * SF `FullCalendarComponentController.EventObject` (cls:204-213) 의 4 필드 정합 (D4=c "SF 호환")
 * + 신규 시스템에서 필요한 보조 식별자/원시값 6 필드 확장.
 */
data class FemaleEmployeeScheduleEventDto(
    /** SF `recordId` — TeamMemberSchedule.id (backend Long PK). */
    val recordId: Long,
    /** SF `title` — `EmpName(EmpCode) | ...` 콤보 문자열 (FullCalendarComponentController.cls:88-96 1:1). */
    val title: String,
    /** SF `start` — WorkingDate ISO date (yyyy-MM-dd). */
    val start: String,
    /** SF `isClockIn` — CommuteLogId 존재 여부 (출근 등록 완료). */
    val isClockIn: Boolean,

    // -- 신규 시스템 확장 필드 (D4=c) --
    val accountId: Long?,
    val employeeId: Long?,
    val workingType: String?,
    val workingCategory1: String?,
    val workingCategory2: String?,
    val workingCategory3: String?,
)

/**
 * 여사원 일정 캘린더 — 월별 일자별 근무현황 요약.
 *
 * SF `FullCalendarComponentController.EventSummaryObject` (cls:214-233) 의 9 필드 1:1 정합.
 */
data class FemaleEmployeeScheduleSummaryDto(
    val year: Int,
    val month: Int,
    val day: Int,
    /** 근무 예정 (행사 제외) 카운트. */
    val expected: Int?,
    /** 근무 실적 (행사 제외) — CommuteLog 존재 카운트. */
    val actual: Int?,
    /** 행사 근무 예정 카운트. */
    val expectedPromo: Int?,
    /** 행사 근무 실적 카운트. */
    val actualPromo: Int?,
    /** 연차 카운트. */
    val holiday: Int?,
    /** 대휴 카운트. */
    val subHoliday: Int?,
)
