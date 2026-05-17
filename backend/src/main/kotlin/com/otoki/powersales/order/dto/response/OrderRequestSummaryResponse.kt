package com.otoki.powersales.order.dto.response

import com.otoki.powersales.order.entity.OrderRequest
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 본인 주문요청 목록 항목 응답 DTO.
 */
data class OrderRequestSummaryResponse(
    val id: Long,
    val orderRequestNumber: String,
    val clientId: Long,
    val clientName: String,
    val orderDate: LocalDateTime,
    val deliveryDate: LocalDate,
    val totalAmount: BigDecimal,
    val orderRequestStatus: String,
    val isClosed: Boolean,
) {
    companion object {
        fun from(entity: OrderRequest, isClosed: Boolean): OrderRequestSummaryResponse =
            OrderRequestSummaryResponse(
                id = entity.id,
                orderRequestNumber = entity.orderRequestNumber,
                clientId = entity.account!!.id.toLong(),
                clientName = entity.account!!.name ?: "",
                orderDate = entity.orderDate,
                deliveryDate = entity.deliveryDate,
                totalAmount = entity.totalAmount,
                orderRequestStatus = entity.orderRequestStatus.name,
                isClosed = isClosed,
            )
    }
}
