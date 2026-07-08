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
    // 집계 모수 — 해당 월 여사원 통합일정(출근등록)에 등장하는 투입 거래처 수 (distinct).
    val investedAccountCount: Int,
    val targetAmount: Long,
    val actualAmount: Long,
    val progressRate: Double,
    val referenceProgressRate: Double,
    val lastYearAmount: Long,
    val lastYearRatio: Double,
    val channelSales: List<ChannelSalesItem>,
    // 매출 데이터 적재 여부 — 0원이 "미적재"인지 "실제 0"인지 구분. false 면 화면에서 "—" 표시.
    val hasActualData: Boolean,
    val hasLastYearData: Boolean,
    // 당월 목표 등록 여부 — 투입 거래처 중 당월 목표 row 가 전무하면 false. false 면 화면에서 "—" 표시
    // (계산은 목표 0 으로 처리 — progressRate 0.0).
    val hasTargetData: Boolean
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

/** etc = jobCode 가 판촉직/OSC직/레이디직 어디에도 해당하지 않거나 null 인 사원 수 (모수 정합용). */
data class StaffTypeCount(
    val promotion: Int,
    val osc: Int,
    val etc: Int
)

/** etc = status 가 재직/휴직 어디에도 해당하지 않거나 null 인 사원 수 (모수 정합용). */
data class TotalByPosition(
    val active: Int,
    val onLeave: Int,
    val etc: Int
)

data class AgeGroupCount(
    val ageGroup: String,
    val count: Int
)

/** 근무형태(고정/격고/순회)별 환산인원 SUM — MFEIS ConvertedHeadcount__c(Number 18,4) 정합. */
data class WorkTypeStats(
    val fixed: BigDecimal,
    val alternating: BigDecimal,
    val visiting: BigDecimal
)
