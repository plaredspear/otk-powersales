package com.otoki.powersales.domain.activity.order.dto.response

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import java.math.BigDecimal

/**
 * 주문 등록 응답 (Spec #592 §5.2).
 */
data class OrderRequestCreateResponse(
    val orderRequestId: Long,
    val orderRequestNumber: String,
    // 상태는 코드(영문)와 한글 표시명을 분리해 내려준다 (목록/상세/취소와 정합).
    // SF nillable=true 정합으로 status/totalAmount 가 nullable (마이그 SF NULL row 보존).
    val status: String?,
    val statusName: String?,
    val totalAmount: BigDecimal?,
) {
    companion object {
        fun from(entity: OrderRequest): OrderRequestCreateResponse = OrderRequestCreateResponse(
            orderRequestId = entity.id,
            orderRequestNumber = entity.orderRequestNumber,
            status = entity.orderRequestStatus?.name,
            statusName = entity.orderRequestStatus?.clientDisplayName,
            totalAmount = entity.totalAmount,
        )
    }
}
