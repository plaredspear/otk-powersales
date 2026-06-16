package com.otoki.powersales.domain.sales.dto.response

/**
 * 월매출 대시보드 하단 거래처 명세 응답.
 *
 * 페이징 + 정렬 + 필터를 적용한 거래처별 row 목록. row 별 카테고리 4종 (상온/라면/냉동냉장/유지) 마감실적 + 진도율 + 전년 동월 비교 + 마감 상태를 포함한다.
 */
data class MonthlySalesDashboardListResponse(
    val items: List<MonthlySalesDashboardListItem>,
    val pageInfo: PageInfo,
) {

    data class PageInfo(
        val page: Int,
        val size: Int,
        val totalElements: Long,
        val totalPages: Int,
    )
}

data class MonthlySalesDashboardListItem(
    val accountId: Long,
    val accountName: String?,
    val sapAccountCode: String?,
    val branchCode: String?,
    val branchName: String?,
    val salesYear: Int,
    val salesMonth: Int,
    val targetAmount: Long?,
    val totalAchievedAmount: Long?,
    val achievementRate: Double?,
    val ambientTargetAmount: Long?,
    val ambientAchievedAmount: Long?,
    val noodleTargetAmount: Long?,
    val noodleAchievedAmount: Long?,
    val frozenRefrigeratedTargetAmount: Long?,
    val frozenRefrigeratedAchievedAmount: Long?,
    val oilFatTargetAmount: Long?,
    val oilFatAchievedAmount: Long?,
    val lastYearAchievedAmount: Long?,
    val lastYearComparisonRatio: Double?,
    val isConfirmed: Boolean,
)
