package com.otoki.internal.dto.response

/**
 * 주문 취소 응답 DTO
 */
data class OrderCancelResponse(
    val cancelledCount: Int,
    val cancelledProductCodes: List<String>
)
