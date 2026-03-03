package com.otoki.internal.admin.dto.response

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
    val count: Int
)

data class WorkTypeCount(
    val workType: String,
    val count: Int
)

data class ChannelWorkTypeItem(
    val channelName: String,
    val fixed: Int,
    val alternating: Int,
    val visiting: Int
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
