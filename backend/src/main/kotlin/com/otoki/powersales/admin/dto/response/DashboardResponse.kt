package com.otoki.powersales.admin.dto.response

import java.math.BigDecimal

data class DashboardResponse(
    val salesSummary: SalesSummary,
    val staffDeployment: StaffDeployment,
    val basicStats: BasicStats
)

data class SalesSummary(
    val yearMonth: String,
    val branchName: String?,
    val targetAmount: Long,
    val actualAmount: Long,
    val progressRate: Double,
    val referenceProgressRate: Double,
    val lastYearAmount: Long,
    val lastYearRatio: Double,
    val channelSales: List<ChannelSalesItem>
)

data class ChannelSalesItem(
    val channelName: String,
    val targetAmount: Long,
    val actualAmount: Long,
    val progressRate: Double
)

data class StaffDeployment(
    val yearMonth: String,
    val branchName: String?,
    val byAccountType: List<AccountTypeCount>,
    val byWorkType: List<WorkTypeCount>,
    val byChannelAndWorkType: List<ChannelWorkTypeItem>,
    val previousMonth: PreviousMonthData
)

data class AccountTypeCount(
    val accountType: String,
    val count: Int,
    // 환산인원 (소수) — SF `ConvertedHeadcount__c` 정합 (scale=4). 차트는 본 필드 사용 (결정 D5)
    val convertedHeadcount: BigDecimal
)

data class WorkTypeCount(
    val workType: String,
    val count: Int,
    val convertedHeadcount: BigDecimal
)

data class ChannelWorkTypeItem(
    val channelName: String,
    val fixed: Int,
    val alternating: Int,
    val visiting: Int,
    // 근무형태(고정/격고/순회)별 환산인원 (소수, scale=4) — 결정 D5
    val fixedHeadcount: BigDecimal,
    val alternatingHeadcount: BigDecimal,
    val visitingHeadcount: BigDecimal
)

data class PreviousMonthData(
    val byWorkType: List<WorkTypeCount>
)

data class BasicStats(
    val branchName: String?,
    val staffType: StaffTypeCount,
    val totalByPosition: TotalByPosition,
    val byAgeGroup: List<AgeGroupCount>,
    val byWorkType: WorkTypeStats
)

data class StaffTypeCount(
    val promotion: Int,
    val osc: Int
)

data class TotalByPosition(
    val active: Int,
    val onLeave: Int
)

data class AgeGroupCount(
    val ageGroup: String,
    val count: Int
)

data class WorkTypeStats(
    val fixed: Int,
    val alternating: Int,
    val visiting: Int
)
