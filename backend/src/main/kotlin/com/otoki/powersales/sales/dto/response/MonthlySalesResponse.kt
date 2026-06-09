package com.otoki.powersales.sales.dto.response

/**
 * 월매출 조회 응답 DTO
 */
data class MonthlySalesResponse(
    val customerId: String,
    val customerName: String,
    val yearMonth: String,
    val targetAmount: Long,
    val achievedAmount: Long,
    val achievementRate: Double,
    /**
     * 기준 진도율 (영업일 기준, %) — 레거시 `calcBusinessRateOnlyThisMonth` 정합.
     * 조회월이 시스템 당월일 때만 `(월초~오늘 영업일) / (월초~월말 영업일) × 100`, 그 외 월은 0.
     */
    val baseRate: Double,
    val categorySales: List<CategorySalesInfo>,
    val yearComparison: YearComparisonInfo,
    val monthlyAverage: MonthlyAverageInfo
) {

    /**
     * 제품유형별 매출 정보
     */
    data class CategorySalesInfo(
        val category: String,
        val targetAmount: Long,
        val achievedAmount: Long,
        val achievementRate: Double
    )

    /**
     * 전년 동월 비교 정보
     */
    data class YearComparisonInfo(
        val currentYear: Long,
        val previousYear: Long
    )

    /**
     * 월 평균 실적 정보
     */
    data class MonthlyAverageInfo(
        val currentYearAverage: Long,
        val previousYearAverage: Long,
        val startMonth: Int,
        val endMonth: Int
    )
}
