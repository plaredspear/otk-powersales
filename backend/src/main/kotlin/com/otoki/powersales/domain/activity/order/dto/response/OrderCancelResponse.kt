package com.otoki.powersales.domain.activity.order.dto.response

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import java.time.LocalDateTime
import java.math.BigDecimal

/**
 * 주문 취소 응답 DTO (Spec #597 §5).
 */
data class OrderCancelResponse(
    val orderRequestId: Long,
    val orderRequestNumber: String,
    // 상태는 코드(영문)와 한글 표시명을 분리해 내려준다 (목록/상세와 정합).
    // SF nillable=true 정합으로 orderRequestStatus 가 nullable — 마이그 SF NULL row 는 두 필드 모두 null.
    val orderRequestStatus: String?,
    val orderRequestStatusName: String?,
    val cancelledLines: List<CancelledLineResponse>,
) {
    companion object {
        fun of(orderRequest: OrderRequest, cancelledProducts: List<OrderRequestProduct>): OrderCancelResponse {
            return OrderCancelResponse(
                orderRequestId = orderRequest.id,
                orderRequestNumber = orderRequest.orderRequestNumber,
                orderRequestStatus = orderRequest.orderRequestStatus?.name,
                orderRequestStatusName = orderRequest.orderRequestStatus?.clientDisplayName,
                cancelledLines = cancelledProducts.map(CancelledLineResponse::from),
            )
        }
    }
}

data class CancelledLineResponse(
    val orderProductId: Long,
    // lineNumber / productCode 는 SF nillable=true 정합으로 nullable (마이그 SF NULL row 보존).
    val lineNumber: BigDecimal?,
    val productCode: String?,
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
