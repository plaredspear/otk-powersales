package com.otoki.powersales.schedule.dto.response

/**
 * 조장 여사원 일별 현황 응답 DTO.
 *
 * 레거시 `employee/mngDaily.jsp` (조장이 특정 날짜의 팀 여사원 진열/행사/연차 근무 현황 +
 * 거래처별 출근 등록 현황을 조회) 의 backend 이식 — **조회 전용**.
 * 일정변경(mutation) 은 별도 작업(P7 / spec #679)으로 분리.
 *
 * 진열/행사 구분은 `workingType=WORK` + `workingCategory1`(진열/행사), 연차는 `workingType=연차`.
 * 출근 여부는 `attendance_log` FK 채워짐 여부로 판정 (FemaleEmployeeScheduleQueryService 정합).
 */
data class LeaderDailyStatusResponse(
    val date: String,
    val summary: LeaderDailyStatusSummary,
    val displayWorkers: List<LeaderDailyWorkerItem>,
    val eventWorkers: List<LeaderDailyWorkerItem>,
    val annualLeaveWorkers: List<LeaderDailyEmployeeItem>,
)

/**
 * 상단 요약 — 진열/행사 출근수·전체수, 연차 인원수 (레거시 mngDaily 상단 헤더 동등).
 */
data class LeaderDailyStatusSummary(
    val displayTotal: Int,
    val displayAttended: Int,
    val eventTotal: Int,
    val eventAttended: Int,
    val annualLeaveCount: Int,
)

/**
 * 진열/행사 근무자 1건(여사원 × 거래처 일정).
 */
data class LeaderDailyWorkerItem(
    val scheduleId: Long,
    /** 진열 거래처 대리출근 등록용 진열 마스터 ID(진열 행만 채워짐). 행사 행은 null. */
    val displayWorkScheduleId: Long?,
    val employeeId: Long?,
    val employeeName: String,
    val employeeCode: String,
    val accountName: String,
    val accountCode: String,
    val workingCategory1: String?,
    val workingCategory2: String?,
    val workingCategory3: String?,
    val attended: Boolean,
)

/**
 * 연차 여사원 항목 (이름/사번만 표시).
 */
data class LeaderDailyEmployeeItem(
    val employeeId: Long?,
    val employeeName: String,
    val employeeCode: String?,
)
