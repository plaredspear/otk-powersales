package com.otoki.powersales.domain.activity.order.dto.response

import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import java.math.BigDecimal

/**
 * 본인 주문요청 상세 — CRM 주문 라인 (`OrderRequestProduct` 매핑) (Spec #595).
 */
data class OrderedItemResponse(
    val orderProductId: Long,
    val productCode: String,
    val productName: String?,
    val totalQuantityBoxes: BigDecimal,
    val totalQuantityPieces: BigDecimal,
    val isCancelled: Boolean,
) {
    companion object {
        fun from(item: OrderRequestProduct): OrderedItemResponse =
            OrderedItemResponse(
                orderProductId = item.id,
                productCode = item.productCode,
                productName = item.product?.name,
                totalQuantityBoxes = item.quantityBoxes,
                totalQuantityPieces = item.quantityPieces,
                isCancelled = item.isCancelled(),
            )
    }
}
