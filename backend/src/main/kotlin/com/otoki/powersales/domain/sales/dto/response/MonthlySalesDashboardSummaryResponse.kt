package com.otoki.powersales.domain.sales.dto.response

/**
 * 월매출 대시보드 상단 KPI + 월별 추이 응답.
 *
 * 권한 범위 거래처 N건의 당월 목표 / 실적 / 진도율 합산과
 * 최근 6개월 월별 추이 (목표 / 실적 / 전년 동월) 시계열 데이터를 제공한다.
 */
data class MonthlySalesDashboardSummaryResponse(
    val salesYear: Int,
    val salesMonth: Int,
    val totalTargetAmount: Long,
    val totalAchievedAmount: Long,
    val overallAchievementRate: Double,
    val referenceAchievementRate: Double,
    val totalLastYearAchievedAmount: Long?,
    val lastYearComparisonRatio: Double?,
    val monthlyTrend: List<MonthlyTrendPoint>,
) {

    data class MonthlyTrendPoint(
        val salesYear: Int,
        val salesMonth: Int,
        val targetAmount: Long,
        val achievedAmount: Long,
        val lastYearAchievedAmount: Long?,
    )
}
