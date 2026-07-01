package com.otoki.powersales.domain.sales.dto.response

import java.math.BigDecimal

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
    /**
     * 유통형태 — 거래처상태코드(AccountStatusCode__c) + 거래처유형(Type) 조합 (예: "02 슈퍼").
     * 월별 여사원 통합일정 화면의 "유통형태" 컬럼과 동일 산식 (`Account.distributionChannelLabel()`).
     * 화면 테이블에는 미표시, 엑셀 다운로드 전용. 두 값 모두 비어있으면 null.
     */
    val distributionChannelLabel: String?,
    /**
     * 거래처유형 — ABC유형코드(ABCTypeCode__c) + ABC유형(ABCType__c) 조합 (예: "6111 이마트").
     * `Account.abcTypeLabel()` 산식 (거래처타입 [accountType] enum 필드와는 별개).
     * 화면 테이블에는 미표시, 엑셀 다운로드 전용. 두 값 모두 비어있으면 null.
     */
    val abcTypeLabel: String?,
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
    /**
     * 해당 월 거래처에 투입된 판매여사원 진열 환산인원 (월 통합일정 기준).
     * MFEIS(월별 여사원 통합일정) `workingCategory1 = '진열'` row 의 `convertedHeadcount` 합.
     * 상시/임시·위탁 필터 미적용 (전체 포함), 거래처(account_id) 기준. 투입 없으면 0.
     */
    val displayHeadcount: BigDecimal,
    /**
     * 해당 월 거래처에 투입된 판매여사원 행사 환산인원.
     * MFEIS `workingCategory1 = '행사'` row 의 `convertedHeadcount` 합. 투입 없으면 0.
     */
    val eventHeadcount: BigDecimal,
    /**
     * 총 환산인원 = 진열 + 행사 (`displayHeadcount + eventHeadcount`).
     * SF 레거시에 월매출 화면 환산인원 결합 선례 없는 신규 조합 (배치적합성 화면 산식 이식).
     */
    val totalHeadcount: BigDecimal,
)
