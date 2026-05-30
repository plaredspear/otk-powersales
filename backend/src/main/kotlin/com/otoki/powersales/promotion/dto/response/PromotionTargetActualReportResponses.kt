package com.otoki.powersales.promotion.dto.response

import java.math.BigDecimal

/**
 * 행사사원 목표 대비 실적 보고서 응답 (Spec #845).
 *
 * 레거시 매핑: SF Report `new_report_AtQ` (영업지원실용·Summary·도넛 차트).
 * 행사명(promotion.name) 그룹 + 그룹별 소계 + 전체 합계 + 행사명별 실적금액 차트(도넛).
 */
data class PromotionTargetActualReportResponse(
    val startDate: String,
    val endDate: String,
    val groups: List<PromotionTargetActualReportGroup>,
    val totalTargetAmount: Long,
    val totalActualAmount: BigDecimal,
    val totalPrimaryQuantity: BigDecimal,
    val totalOtherQuantity: BigDecimal,
    val chart: List<PromotionTargetActualChartItem>,
)

/** 행사명 그룹 — rows + 그룹 소계. */
data class PromotionTargetActualReportGroup(
    val promotionName: String?,
    val subtotalTargetAmount: Long,
    val subtotalActualAmount: BigDecimal,
    val subtotalPrimaryQuantity: BigDecimal,
    val subtotalOtherQuantity: BigDecimal,
    val rows: List<PromotionTargetActualReportRow>,
)

/** 도넛 차트 1항목 — 행사명별 실적금액 합계 ('지점별 행사실적 구성 현황'). */
data class PromotionTargetActualChartItem(
    val promotionName: String?,
    val actualAmount: BigDecimal,
)

/**
 * 목표/실적 1행 (23컬럼) — promotionEmployee × promotion × account × product × employee × teamMemberSchedule.
 *
 * actualAmount = SF Formula `DailyActualSalesAmount__c` 재현(dkDailyActualSalesAmount 파생).
 * isWorkReport/commuteDate 는 teamMemberSchedule 조인.
 */
data class PromotionTargetActualReportRow(
    val promotionName: String?,
    val branchName: String?,
    val accountName: String?,
    val accountCode: String?,
    val primaryProductName: String?,
    val category1: String?,
    val otherProduct: String?,
    val employeeCode: String?,
    val employeeOrgName: String?,
    val employeeName: String?,
    val professionalPromotionTeam: String?,
    val scheduleDate: String?,
    val targetAmount: Long?,
    val actualAmount: BigDecimal?,
    val standLocation: String?,
    val primarySalesQuantity: BigDecimal?,
    val primaryProductAmount: BigDecimal?,
    val otherSalesQuantity: BigDecimal?,
    val otherSalesAmount: BigDecimal?,
    val workType2: String?,
    val workType3: String?,
    val isWorkReport: String?,
    val commuteDate: String?,
)
