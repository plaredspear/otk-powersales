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

/**
 * 여사원 투입현황 — SF 레거시 대시보드 리포트(`근무형태별(상세) 환산인원현황(진열)/(행사)`) 정합.
 *
 * SF 는 근무유형1(진열/행사)별로 리포트 2개(가로 누적 막대)를 노출한다. 각 차트는
 * 행그룹 = 거래처유형(구분), 열그룹 = 근무형태3&4(`WorkTypeForReport__c`), 측정값 = SUM(환산인원).
 * 조회월(yearMonth)의 **전월(마감)** MFEIS 데이터 기준 (LAST_MONTH 필터).
 * yearMonth 는 조회 조건 echo (데이터 기준월은 그 전월).
 *
 * @param display 진열 차트 (스택 = 1.고정/2.격고/3.순회 — 근무유형3 기반)
 * @param event   행사 차트 (스택 = 4.상온/5.냉동/5.냉장/... — 근무유형4 기반, 값 가변)
 */
data class StaffDeployment(
    val yearMonth: String,
    val branchName: String?,
    val display: WorkTypeChannelChart,
    val event: WorkTypeChannelChart
)

/**
 * 유통(거래처유형) × 근무형태 스택 누적 막대 1개 — SF 리포트 1개 대응.
 *
 * [stackKeys] 는 스택 세그먼트 라벨 순서(SF WorkTypeForReport__c 값의 정렬 순서 — "1.고정","2.격고"...).
 * [rows] 는 거래처유형별 1행이며, 각 행의 [ChannelStackRow.headcounts] 가 stackKeys 와 같은 순서로 대응한다.
 */
data class WorkTypeChannelChart(
    val stackKeys: List<String>,
    val rows: List<ChannelStackRow>,
    // 차트 전체 환산인원 합계 (SF "총 환산인원 합계") — scale=4
    val totalHeadcount: BigDecimal
)

/**
 * 거래처유형(유통) 1행 — 스택 세그먼트별 환산인원.
 *
 * @param channelName 거래처유형 displayName (SF AccountType__c). 미상은 "미상".
 * @param headcounts  차트 [WorkTypeChannelChart.stackKeys] 와 동일 순서의 환산인원 SUM 리스트 (scale=4).
 */
data class ChannelStackRow(
    val channelName: String,
    val headcounts: List<BigDecimal>
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

data class BasicStats(
    val branchName: String?,
    val staffType: StaffTypeCount,
    val totalByPosition: TotalByPosition,
    val byAgeGroup: List<AgeGroupCount>,
    val byWorkType: WorkTypeStats
)

/**
 * etc = jobCode 가 판촉직/OSC직/레이디직 어디에도 해당하지 않거나 null 인 사원 수 (모수 정합용).
 * etcBreakdown = "기타" 를 구성하는 원본 jobCode 값별 세부 내역 (툴팁 표시용, count 내림차순).
 */
data class StaffTypeCount(
    val promotion: Int,
    val osc: Int,
    val etc: Int,
    val etcBreakdown: List<EtcBreakdownItem>
)

/**
 * etc = status 가 재직/휴직 어디에도 해당하지 않거나 null 인 사원 수 (모수 정합용).
 * etcBreakdown = "기타" 를 구성하는 원본 status 값별 세부 내역 (툴팁 표시용, count 내림차순).
 */
data class TotalByPosition(
    val active: Int,
    val onLeave: Int,
    val etc: Int,
    val etcBreakdown: List<EtcBreakdownItem>
)

/** "기타" 항목 세부 내역 1건 — 원본 값(label)과 인원 수(count). null/공백 값은 "미분류" 로 표기. */
data class EtcBreakdownItem(
    val label: String,
    val count: Int
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
