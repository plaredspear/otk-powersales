package com.otoki.powersales.domain.sales.dto.response

/**
 * 물류매출 조회 응답 DTO.
 *
 * ## 데이터 source
 * ORORA view `ECRM_ABCCUST_MH_V` ([com.otoki.orora.entity.OroraMonthlySalesHistory]) 의
 * `ShipClosingAmount1~3` (물류마감실적 — 상온/라면/냉장냉동). SF `MonthlySalesHistory__c`
 * 레거시 동등물이며 레거시 `promotion/month/list.jsp` 가 표시하던 바로 그 소스.
 *
 * ## 온도대 매핑 (레거시 deviation 박제)
 * - `ShipClosingAmount1` → NORMAL (상온)
 * - `ShipClosingAmount2` → RAMEN (라면)
 * - `ShipClosingAmount3` → FROZEN (냉장냉동)
 * - `ShipClosingAmount4` (유지) → **미포함**: 모바일 물류매출 화면이 3개 온도대 탭만 보유
 *   (상온/라면/냉동·냉장). 레거시는 4종이나 현 모바일 UI 범위에 맞춰 3종만 반환.
 */
data class LogisticsSalesResponse(
    val customerId: Long,
    val customerName: String,
    val sapAccountCode: String,
    val yearMonth: String,

    /** 조회 연월이 시스템 당월인지 여부 (UI 배지: 당월 vs 이전월 마감실적). */
    val isCurrentMonth: Boolean,

    val categories: List<CategorySales>
) {
    /**
     * 온도대별 물류매출 실적.
     *
     * `previousYearAmount` 은 전년 동월 [com.otoki.orora.entity.OroraMonthlySalesHistory]
     * 의 동일 온도대 ShipClosing 값.
     */
    data class CategorySales(
        /** 온도대 코드 — 모바일 `LogisticsCategory` 정합: NORMAL / RAMEN / FROZEN. */
        val category: String,
        val currentAmount: Long,
        val previousYearAmount: Long,
        val difference: Long,
        /** 전년 대비 증감율(%). 전년 실적 0 이면 0.0. */
        val growthRate: Double
    )
}
