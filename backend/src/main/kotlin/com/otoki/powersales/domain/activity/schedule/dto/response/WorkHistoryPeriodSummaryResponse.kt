package com.otoki.powersales.domain.activity.schedule.dto.response

/**
 * 기간별 근무내역(개인) — 여사원별 근무 집계 항목.
 *
 * 근무기간 조회(월별근무내역 목록)와 동일하게 [TeamMemberSchedule] 을 원천으로 하되,
 * 단일 월이 아닌 기간(시작년월~종료년월) 전체를 여사원별로 집계해 1행으로 제공한다.
 * 통합일정(월별여사원 통합일정)과 유사한 "여사원별 1행" 레이아웃이나, 집계 원천이 MFEIS 가 아닌
 * TeamMemberSchedule 일자별 실적이라는 점이 다르다.
 */
data class WorkHistoryPeriodSummaryItem(
    /** 소속지점명 (Employee.orgName). */
    val orgName: String?,
    /** 사번 (Employee.employeeCode). */
    val employeeCode: String?,
    /** 이름 (Employee.name). */
    val employeeName: String?,
    /** 직위 (Employee.jikwee). */
    val title: String?,
    /** 총 근무일수 (출근 등록된 일정 행 수). */
    val totalWorkingDays: Int,
    /** 근무 거래처 수 (distinct account). */
    val workingAccountCount: Int,
    /** 근무유형(WorkingCategory1)별 일수 — 진열. */
    val displayDays: Int,
    /** 근무유형(WorkingCategory1)별 일수 — 행사. */
    val eventDays: Int,
    /** 구분(WorkingType)별 일수 — 근무. */
    val workDays: Int,
    /** 구분(WorkingType)별 일수 — 연차. */
    val annualLeaveDays: Int,
    /** 구분(WorkingType)별 일수 — 대휴. */
    val altHolidayDays: Int,
)

data class WorkHistoryPeriodSummaryResponse(
    val fromYearMonth: String,
    val toYearMonth: String,
    val items: List<WorkHistoryPeriodSummaryItem>,
    val totalCount: Int,
)
