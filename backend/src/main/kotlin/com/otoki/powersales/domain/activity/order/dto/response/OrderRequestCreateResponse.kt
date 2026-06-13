package com.otoki.powersales.domain.activity.order.dto.response

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import java.math.BigDecimal

/**
 * 주문 등록 응답 (Spec #592 §5.2).
 */
data class OrderRequestCreateResponse(
    val orderRequestId: Long,
    val orderRequestNumber: String,
    val status: OrderRequestStatus,
    val totalAmount: BigDecimal,
) {
    companion object {
        fun from(entity: OrderRequest): OrderRequestCreateResponse = OrderRequestCreateResponse(
            orderRequestId = entity.id,
            orderRequestNumber = entity.orderRequestNumber,
            status = entity.orderRequestStatus,
            totalAmount = entity.totalAmount,
        )
    }
}
