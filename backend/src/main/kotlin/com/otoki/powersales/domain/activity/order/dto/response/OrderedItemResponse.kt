package com.otoki.powersales.domain.activity.order.dto.response

import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import java.math.BigDecimal

/**
 * 본인 주문요청 상세 — CRM 주문 라인 (`OrderRequestProduct` 매핑) (Spec #595).
 *
 * `isOutOfStock` / `outOfStockReason` 은 SAP `OrderRequestDetail` 응답에서 해당 productCode 가
 * `DefaultReason` (결품 사유) 를 가진 경우 채워진다. 레거시 화면 `view.jsp:414` 동등 — 결품 제품을
 * "주문한 제품" 리스트에 회색+사유로 표시 (SAP 처리현황 그룹에는 넣지 않음).
 */
data class OrderedItemResponse(
    val orderProductId: Long,
    val productCode: String?,
    val productName: String?,
    val totalQuantityBoxes: BigDecimal,
    val totalQuantityPieces: BigDecimal,
    val isCancelled: Boolean,
    val isOutOfStock: Boolean = false,
    val outOfStockReason: String? = null,
) {
    companion object {
        /**
         * @param forceCancelled 상세조회 정합(Spec #858)으로 취소 승격된 라인이면 true — 조회 트랜잭션의
         *   엔티티는 정합(별도 REQUIRES_NEW 트랜잭션) 반영 전 상태이므로, 승격된 productCode 는
         *   `isCancelled` 를 강제로 true 로 반영한다.
         */
        fun from(
            item: OrderRequestProduct,
            productName: String? = null,
            outOfStockReason: String? = null,
            forceCancelled: Boolean = false,
        ): OrderedItemResponse =
            OrderedItemResponse(
                orderProductId = item.id,
                productCode = item.productCode,
                productName = productName ?: item.product?.name,
                // SF nillable=true 정합으로 수량이 nullable — 응답은 0 으로 보정 (기존 의미 보존).
                totalQuantityBoxes = item.quantityBoxes ?: BigDecimal.ZERO,
                totalQuantityPieces = item.quantityPieces ?: BigDecimal.ZERO,
                isCancelled = item.isCancelled() || forceCancelled,
                isOutOfStock = outOfStockReason != null,
                outOfStockReason = outOfStockReason,
            )
    }
}
