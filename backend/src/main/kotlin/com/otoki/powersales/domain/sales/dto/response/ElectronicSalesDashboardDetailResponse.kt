package com.otoki.powersales.domain.sales.dto.response

/**
 * 전산실적 대시보드 단건 거래처 상세 — 제품별 명세.
 *
 * 「월 매출(전산실적)」 명세 테이블 row 클릭 시 표시. 레거시 `abcmain.jsp` 의 제품별 조회
 * (`SelectAbcData` — `GROUP BY ITEM_CD`) 동등. 거래처 1곳의 기간(startDate~endDate) 제품별 매출
 * 금액/수량 — 목록의 제품/분류 필터가 지정된 경우 동일 필터를 반영해 목록 행 합계와 정합.
 */
data class ElectronicSalesDashboardDetailResponse(
    val customerId: Long,
    val customerName: String?,
    val sapAccountCode: String?,
    val startDate: java.time.LocalDate,
    val endDate: java.time.LocalDate,
    val totalAmount: Long,
    val totalQuantity: Long,
    val items: List<ProductSales>,
) {
    /**
     * @property productCode 제품코드 (`ITEM_CD`)
     * @property productName 제품명 (`ITEM_NM`)
     * @property amount 매출 금액 (원) — `SUM(SALES_RAMT)`
     * @property quantity 매출 수량 — `SUM(SALES_RQTY)`
     */
    data class ProductSales(
        val productCode: String,
        val productName: String,
        val amount: Long,
        val quantity: Long,
    )
}
