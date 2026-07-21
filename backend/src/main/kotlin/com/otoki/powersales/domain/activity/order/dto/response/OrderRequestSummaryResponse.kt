package com.otoki.powersales.domain.activity.order.dto.response

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
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
    // orderDate / deliveryDate / totalAmount / orderRequestStatus 는 SF nillable=true 정합으로 nullable (마이그 SF NULL row 보존).
    val orderDate: LocalDateTime?,
    val deliveryDate: LocalDate?,
    val totalAmount: BigDecimal?,
    // 상태는 코드(영문)와 한글 표시명을 분리해 내려준다. 모바일은 표시명을 그대로 출력하고
    // 코드는 색상/분기 로직에만 사용한다 (클라이언트 enum 매핑 제거).
    val orderRequestStatus: String?,
    val orderRequestStatusName: String?,
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
                orderRequestStatus = entity.orderRequestStatus?.name,
                orderRequestStatusName = entity.orderRequestStatus?.clientDisplayName,
                isClosed = isClosed,
            )
    }
}
