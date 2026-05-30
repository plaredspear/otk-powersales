package com.otoki.powersales.schedule.dto.response

import java.math.BigDecimal

/**
 * 거래처유형별 환산인원 현황 보고서 응답 (Spec #847).
 *
 * 레거시 매핑: SF Report 5변형 (`new_report_nNq` 1-1 / `X12_only_aM6` 1-2 / `X14_cwi` 1-4 /
 * `X15_Uyw` 1-5 / `X21_E94` (2팀)2-1). 기준 객체 MonthlyFemaleEmployeeIntegrationSchedule__c.
 * 집계 = SUM(ConvertedHeadcount__c), 그룹핑 = 구분(AccountType) × 근무유형1 × 지점 × 연월.
 * variant 별 근무유형5 / 위탁제외 / 2팀(CostCenterCode) 필터 차이. 전사 스코프.
 */
data class ConvertedHeadcountReportResult(
    val variant: String,
    val year: String,
    val month: String,
    val groups: List<ConvertedHeadcountReportGroup>,
    val totalHeadcount: BigDecimal,
)

/**
 * 구분(거래처유형) 그룹 — SF groupingsAcross 1단(AccountType) 정합.
 */
data class ConvertedHeadcountReportGroup(
    val accountType: String,
    val subtotalHeadcount: BigDecimal,
    val rows: List<ConvertedHeadcountReportRow>,
)

/**
 * 환산인원 집계 1행 — 구분 × 근무유형1 × 지점 × 연월 × SUM(환산인원).
 *
 * branchName = variant 별 지점 기준 (1-x: 여사원 소속 empBranchName / 2-1: 거래처 account.branchName).
 */
data class ConvertedHeadcountReportRow(
    val accountType: String?,
    val workingCategory1: String?,
    val branchName: String?,
    val yearMonth: String?,
    val convertedHeadcount: BigDecimal,
)
