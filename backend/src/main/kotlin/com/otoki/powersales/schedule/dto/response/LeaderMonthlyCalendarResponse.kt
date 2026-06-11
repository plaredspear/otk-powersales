package com.otoki.powersales.schedule.dto.response

/**
 * 조장 여사원 월간 일정 캘린더 응답 DTO (레거시 `employee/mgnSchedule.jsp` + `calSchedule` 동등).
 *
 * 레거시 캘린더는 일정이 있는 날짜마다 `출근완료수 / 전체수`(sum/cnt)를 표시한다.
 * - 전체 모드(employeeId 미지정): 조 전체 여사원의 일자별 (진열[안전점검 제출]+행사) 근무 건수/출근 건수.
 * - 개인 모드(employeeId 지정): 해당 조원의 일자별 근무 건수/출근 건수.
 * 일정이 없는(total=0) 날짜는 days 에 포함하지 않는다(레거시 cnt>0 만 표시).
 */
data class LeaderMonthlyCalendarResponse(
    val year: Int,
    val month: Int,
    val days: List<LeaderCalendarDay>
)

/**
 * 일자별 출근 집계.
 * @property date YYYY-MM-DD
 * @property total 전체 근무 건수(레거시 cnt)
 * @property attended 출근완료 건수(레거시 sum)
 */
data class LeaderCalendarDay(
    val date: String,
    val total: Int,
    val attended: Int
)
