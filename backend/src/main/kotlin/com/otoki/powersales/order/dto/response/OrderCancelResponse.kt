package com.otoki.powersales.order.dto.response

import com.otoki.powersales.order.entity.OrderRequest
import com.otoki.powersales.order.entity.OrderRequestProduct
import com.otoki.powersales.order.enums.OrderRequestStatus
import java.time.LocalDateTime

/**
 * 주문 취소 응답 DTO (Spec #597 §5).
 */
data class OrderCancelResponse(
    val orderRequestId: Long,
    val orderRequestNumber: String,
    val orderRequestStatus: OrderRequestStatus,
    val cancelledLines: List<CancelledLineResponse>,
) {
    companion object {
        fun of(orderRequest: OrderRequest, cancelledProducts: List<OrderRequestProduct>): OrderCancelResponse {
            return OrderCancelResponse(
                orderRequestId = orderRequest.id,
                orderRequestNumber = orderRequest.orderRequestNumber,
                orderRequestStatus = orderRequest.orderRequestStatus,
                cancelledLines = cancelledProducts.map(CancelledLineResponse::from),
            )
        }
    }
}

data class CancelledLineResponse(
    val orderProductId: Long,
    val lineNumber: Long,
    val productCode: String,
    val cancelledAt: LocalDateTime?,
) {
    companion object {
        fun from(product: OrderRequestProduct): CancelledLineResponse {
            return CancelledLineResponse(
                orderProductId = product.id,
                lineNumber = product.lineNumber,
                productCode = product.productCode,
                cancelledAt = product.cancelledAt,
            )
        }
    }
}
