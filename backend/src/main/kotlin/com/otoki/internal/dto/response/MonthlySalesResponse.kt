package com.otoki.internal.dto.response

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
