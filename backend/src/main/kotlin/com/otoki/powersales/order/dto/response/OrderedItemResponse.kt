package com.otoki.powersales.order.dto.response

import com.otoki.powersales.order.entity.OrderRequestProduct
import java.math.BigDecimal

/**
 * 본인 주문요청 상세 — CRM 주문 라인 (`OrderRequestProduct` 매핑) (Spec #595).
 */
data class OrderedItemResponse(
    val productCode: String,
    val productName: String,
    val totalQuantityBoxes: BigDecimal,
    val totalQuantityPieces: Int,
    val isCancelled: Boolean,
) {
    companion object {
        fun from(item: OrderRequestProduct): OrderedItemResponse =
            OrderedItemResponse(
                productCode = item.productCode,
                productName = item.productName,
                totalQuantityBoxes = item.quantityBoxes,
                totalQuantityPieces = item.quantityPieces,
                isCancelled = item.isCancelled,
            )
    }
}
