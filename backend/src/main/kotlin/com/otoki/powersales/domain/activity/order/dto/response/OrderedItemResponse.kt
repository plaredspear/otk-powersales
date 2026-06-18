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
    val productCode: String,
    val productName: String?,
    val totalQuantityBoxes: BigDecimal,
    val totalQuantityPieces: BigDecimal,
    val isCancelled: Boolean,
    val isOutOfStock: Boolean = false,
    val outOfStockReason: String? = null,
) {
    companion object {
        fun from(
            item: OrderRequestProduct,
            outOfStockReason: String? = null,
        ): OrderedItemResponse =
            OrderedItemResponse(
                orderProductId = item.id,
                productCode = item.productCode,
                productName = item.product?.name,
                totalQuantityBoxes = item.quantityBoxes,
                totalQuantityPieces = item.quantityPieces,
                isCancelled = item.isCancelled(),
                isOutOfStock = outOfStockReason != null,
                outOfStockReason = outOfStockReason,
            )
    }
}
