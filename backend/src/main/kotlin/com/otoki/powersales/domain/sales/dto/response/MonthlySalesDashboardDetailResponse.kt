package com.otoki.powersales.domain.sales.dto.response

/**
 * 월매출 대시보드 단건 거래처 상세 응답.
 *
 * 모바일 월매출조회 페이지의 화면 영역 6종 (진도율 바 / 목표·실적 / 카테고리 4종 / 전년 동월 차트 / 전년 평균 차트) 과 동일한 데이터 구조.
 */
data class MonthlySalesDashboardDetailResponse(
    val customerId: Long,
    val customerName: String?,
    val salesYear: Int,
    val salesMonth: Int,
    val targetAmount: Long,
    val achievedAmount: Long,
    val achievementRate: Double,
    val referenceAchievementRate: Double,
    val categorySales: List<CategorySalesInfo>,
    val yearComparison: YearComparisonInfo,
    val monthlyAverage: MonthlyAverageInfo,
) {

    data class CategorySalesInfo(
        val category: String,
        val targetAmount: Long,
        val achievedAmount: Long,
        val achievementRate: Double,
    )

    data class YearComparisonInfo(
        val currentYear: Long,
        val previousYear: Long,
    )

    data class MonthlyAverageInfo(
        val currentYearAverage: Long,
        val previousYearAverage: Long,
        val startMonth: Int,
        val endMonth: Int,
    )
}
