package com.otoki.internal.dto.response

import com.otoki.internal.entity.Order

/**
 * 주문 요약 응답 DTO
 * 내주문 목록에서 사용
 */
data class OrderSummaryResponse(
    val id: Long,
    val orderRequestNumber: String,
    val clientId: Long,
    val clientName: String,
    val clientDeadlineTime: String?,
    val orderDate: String,
    val deliveryDate: String,
    val totalAmount: Long,
    val approvalStatus: String,
    val isClosed: Boolean
) {
    companion object {
        fun from(order: Order): OrderSummaryResponse {
            return OrderSummaryResponse(
                id = order.id,
                orderRequestNumber = order.orderRequestNumber,
                clientId = order.store.id,
                clientName = order.store.storeName,
                clientDeadlineTime = order.clientDeadlineTime,
                orderDate = order.orderDate.toString(),
                deliveryDate = order.deliveryDate.toString(),
                totalAmount = order.totalAmount,
                approvalStatus = order.approvalStatus.name,
                isClosed = order.isClosed
            )
        }
    }
}
