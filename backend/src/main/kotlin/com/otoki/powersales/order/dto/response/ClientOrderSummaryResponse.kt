package com.otoki.powersales.order.dto.response

import com.otoki.powersales.order.entity.ErpOrder

/**
 * 거래처별 주문 목록 응답 DTO
 * SAP 주문번호 기준으로 그룹핑된 주문 요약 정보
 */
data class ClientOrderSummaryResponse(
    val sapOrderNumber: String,
    val clientId: Long,
    val clientName: String,
    val totalAmount: Long
) {
    companion object {
        /**
         * `erp_order` 엔티티 → 거래처별 주문 목록 요약 변환.
         *
         * 레거시 응답 필드 매핑:
         * - SAPOrderNumber → sapOrderNumber
         * - SAPAccountName → clientName (없으면 account.name 폴백)
         * - TotalOrderAmount → totalAmount
         */
        fun from(order: ErpOrder): ClientOrderSummaryResponse {
            return ClientOrderSummaryResponse(
                sapOrderNumber = order.sapOrderNumber,
                clientId = order.account?.id ?: 0L,
                clientName = order.sapAccountName ?: order.account?.name ?: "",
                totalAmount = order.orderSalesAmount?.toLong() ?: 0L
            )
        }
    }
}
