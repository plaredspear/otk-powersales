package com.otoki.powersales.schedule.dto.response

import java.math.BigDecimal

/** 배치적합성 판정 (적합 / 경계 / 재검토). */
enum class Suitability(val displayName: String) {
    FIT("적합"),
    BOUNDARY("경계"),
    REVIEW("재검토");
}

/** 거래처 카테고리 분류 코드 (집계표 열). 체인 거래처는 ABCType 기준으로 분류. */
enum class AccountCategoryColumn(val displayName: String) {
    HYPER("대형마트"),
    NH("농협"),
    CHAIN("체인"),
    SUPER("슈퍼"),
    DEALER("대리점"),
    DEPT("백화점"),
    WHOLESALE("홀세일"),
    MILITARY("군납"),
    FOOD("식자재"),
    OTHER("기타");
}

/** 거래처유형 picklist 항목 — `AccountCategoryMaster.useSearch=true` 항목 1건. */
data class SearchAccountCategoryItem(
    val accountCode: String,
    val name: String
)

/** 집계 조회 응답 — 배치적합성 × 거래처카테고리 거래처 수 집계표. */
data class SalesComparisonSummaryResponse(
    val year: Int,
    val month: Int,
    val rows: List<SalesComparisonSummaryRow>,
    val total: SalesComparisonSummaryRow
)

/** 집계표 한 행 — 배치적합성 1건 (또는 총계) 에 대한 거래처카테고리별 거래처 수. */
data class SalesComparisonSummaryRow(
    val suitability: String,
    val totalCount: Int,
    val countsByCategory: Map<String, Int>,
    val accountIdsByCategory: Map<String, List<Long>>
)

/** 중간집계 조회 응답 — 거래처별 행 + 적합성별 소계 + 전체 총계. */
data class SalesComparisonMiddleResponse(
    val year: Int,
    val month: Int,
    val items: List<SalesComparisonMiddleItem>,
    val subtotals: List<SalesComparisonMiddleSubtotal>,
    val total: SalesComparisonMiddleSubtotal
)

/** 중간집계 한 행 — 거래처 단위. */
data class SalesComparisonMiddleItem(
    val accountId: Long,
    val accountCode: String,
    val accountName: String,
    val accountBranchName: String?,
    val accountCategory: String,
    val suitability: String,
    val avgClosingAmount: Long,
    val totalDisplayHeadcount: Int,
    val totalDisplayConvertedHeadcount: BigDecimal,
    val totalEventConvertedHeadcount: BigDecimal,
    val fixedStandardAmount: BigDecimal?,
    val bifurcationHalfStandardAmount: BigDecimal?,
    val totalInputCount: Int,
    val totalEquivalentWorkingDays: BigDecimal,
    val thisMonthSalesAmount: Long,
    val ediPos: String?
)

/** 중간집계 소계 — 적합성 구분별 또는 전체 총계. */
data class SalesComparisonMiddleSubtotal(
    val suitability: String,
    val accountCount: Int,
    val avgClosingAmount: Long,
    val totalDisplayHeadcount: Int,
    val totalDisplayConvertedHeadcount: BigDecimal,
    val totalEventConvertedHeadcount: BigDecimal,
    val totalInputCount: Int,
    val totalEquivalentWorkingDays: BigDecimal,
    val thisMonthSalesAmount: Long
)

/** 상세 조회 응답 — 사원별 행 + 총계. */
data class SalesComparisonDetailResponse(
    val year: Int,
    val month: Int,
    val items: List<SalesComparisonDetailItem>,
    val total: SalesComparisonDetailTotal
)

/** 상세 한 행 — 사원 × 거래처 단위. */
data class SalesComparisonDetailItem(
    val accountId: Long,
    val accountCode: String,
    val accountName: String,
    val accountBranchName: String?,
    val accountCategory: String,
    val accountCategoryCode: String?,
    val employeeCode: String,
    val employeeName: String,
    val title: String?,
    val workingCategory1: String,
    val workingCategory3: String?,
    val workingCategory4: String?,
    val workingCategory5: String?,
    val suitability: String,
    val avgClosingAmount: Long,
    val totalDisplayHeadcount: Int,
    val totalDisplayConvertedHeadcount: BigDecimal,
    val totalEventConvertedHeadcount: BigDecimal,
    val fixedStandardAmount: BigDecimal?,
    val bifurcationHalfStandardAmount: BigDecimal?,
    val inputCount: Int,
    val equivalentWorkingDays: BigDecimal,
    val convertedHeadcount: BigDecimal,
    val thisMonthSalesAmount: Long,
    val ediPos: String?
)

/** 상세 총계 — 한 응답 안 모든 행의 합계. */
data class SalesComparisonDetailTotal(
    val rowCount: Int,
    val totalDisplayHeadcount: Int,
    val totalDisplayConvertedHeadcount: BigDecimal,
    val totalEventConvertedHeadcount: BigDecimal,
    val totalInputCount: Int,
    val totalEquivalentWorkingDays: BigDecimal,
    val totalConvertedHeadcount: BigDecimal,
    val totalThisMonthSalesAmount: Long
)
