package com.otoki.powersales.domain.activity.schedule.dto.response

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class MonthlyIntegrationScheduleResponse(
    val year: Int,
    val month: Int,
    val items: List<MonthlyIntegrationScheduleItem>,
    val totalCount: Int
)

data class MonthlyIntegrationScheduleItem(
    /** MFEIS row PK — 상세 조회 진입 키. 실시간 집계 경로(buildIntegrationItems)는 null. */
    val id: Long? = null,
    val branchName: String,
    val accountBranchName: String?,
    val accountCode: String,
    val accountName: String,
    /** 유통형태 — 거래처상태코드 + 거래처유형명 조합 (예: "02 슈퍼"). */
    val distributionChannelLabel: String?,
    /** 거래처유형 — ABC유형코드 + ABC유형 조합 (예: "6111 이마트"). */
    val abcTypeLabel: String?,
    val employeeCode: String,
    val title: String?,
    val employeeName: String,
    val workingCategory1: String,
    val workingCategory3: String?,
    val workingCategory4: String?,
    val workingCategory5: String?,
    val totalInputCount: Int,
    val equivalentWorkingDays: BigDecimal,
    val convertedHeadcount: BigDecimal,
    val avgClosingAmount: Long
)

/**
 * MFEIS row 상세 — 집계 근거가 된 여사원일정(TeamMemberSchedule) 목록 포함.
 *
 * 요약 수치(workingDaysMonth/totalInputCount/equivalentWorkingDays/convertedHeadcount)는
 * MFEIS 에 persist 된 집계값 그대로이며, schedules 는 `refreshIntegration` 과 동일한 모수 필터
 * (사원+월 + 출근등록(attendanceLog 연결) + 거래처 존재)에서 본 row 의 집계 키(ExternalKey)에
 * 속하는 일정만 추린 것이다.
 */
data class MonthlyIntegrationDetailResponse(
    val id: Long,
    val year: Int,
    val month: Int,
    val branchName: String?,
    val employeeCode: String?,
    val employeeName: String?,
    val accountCode: String?,
    val accountName: String?,
    val workingCategory1: String?,
    val workingCategory3: String?,
    val workingCategory4: String?,
    val workingCategory5: String?,
    /** 당월 근무일수 — 사원+소속지점 기준 distinct 근무일 수 (거래처 무관). */
    val workingDaysMonth: Int,
    val totalInputCount: Int,
    val equivalentWorkingDays: BigDecimal,
    val convertedHeadcount: BigDecimal,
    val schedules: List<MonthlyIntegrationSourceScheduleItem>,
)

/**
 * 집계 근거 여사원일정 1건.
 *
 * `dailyScheduleCount` = 그날 사원의 출근 일정 수 N (거래처 무관),
 * `equivalentContribution` = 1/N — 총 환산근무일수는 이 기여분의 합.
 */
data class MonthlyIntegrationSourceScheduleItem(
    val scheduleId: Long,
    val workingDate: LocalDate,
    val accountCode: String?,
    val accountName: String?,
    val workingCategory1: String?,
    val workingCategory3: String?,
    val workingCategory4: String?,
    val workingCategory5: String?,
    /** 출근보고 일시 — attendanceLog.attendanceDate (SF CommuteDate formula 동등). */
    val attendanceReportedAt: LocalDateTime?,
    val dailyScheduleCount: Int,
    val equivalentContribution: BigDecimal,
)

data class CategoryScheduleResponse(
    val year: Int,
    val month: Int,
    val items: List<CategoryScheduleItem>
)

data class CategoryScheduleItem(
    val branchName: String,
    val currentMonthTotal: BigDecimal,
    val previousMonthTotal: BigDecimal,
    val totalChange: BigDecimal,
    val displayFixed: BigDecimal,
    val displayAlternate: BigDecimal,
    val displayPatrol: BigDecimal,
    val currentMonthDisplayTotal: BigDecimal,
    val previousMonthDisplayTotal: BigDecimal,
    val displayChange: BigDecimal,
    val eventAmbient: BigDecimal,
    val eventFrozenChilled: BigDecimal,
    val currentMonthEventTotal: BigDecimal,
    val previousMonthEventTotal: BigDecimal,
    val eventChange: BigDecimal
)
