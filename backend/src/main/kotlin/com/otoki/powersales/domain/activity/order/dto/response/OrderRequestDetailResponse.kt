package com.otoki.powersales.domain.activity.order.dto.response

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 본인 주문요청 상세 응답 DTO (Spec #595).
 *
 * 레거시 `IF_REST_MOBILE_OrderRequestDetail.cls` + Heroku JSP 그룹 섹션을 신규 JSON 응답으로 동등 매핑.
 *
 * - `orderProcessingStatusList` 는 SAP 주문번호별 그룹 배열. 마감 전 / SAP 호출 실패 시 `null`.
 * - `rejectedItems` 는 반려/결품 라인 배열. 없으면 `null`.
 */
data class OrderRequestDetailResponse(
    val id: Long,
    val orderRequestNumber: String,
    val clientId: Long,
    val clientName: String,
    val clientDeadlineTime: String?,
    val orderDate: LocalDateTime,
    val deliveryDate: LocalDate,
    val totalAmount: BigDecimal,
    val totalApprovedAmount: BigDecimal,
    val orderRequestStatus: OrderRequestStatus,
    val isClosed: Boolean,
    val orderedItemCount: Int,
    val orderedItems: List<OrderedItemResponse>,
    val orderProcessingStatusList: List<OrderProcessingStatusResponse>?,
    val rejectedItems: List<RejectedItemResponse>?,
) {
    companion object {
        fun of(
            orderRequest: OrderRequest,
            isClosed: Boolean,
            orderedItems: List<OrderedItemResponse>,
            orderProcessingStatusList: List<OrderProcessingStatusResponse>?,
            rejectedItems: List<RejectedItemResponse>?,
        ): OrderRequestDetailResponse =
            OrderRequestDetailResponse(
                id = orderRequest.id,
                orderRequestNumber = orderRequest.orderRequestNumber,
                clientId = orderRequest.account!!.id.toLong(),
                clientName = orderRequest.account!!.name ?: "",
                clientDeadlineTime = orderRequest.clientDeadlineTime,
                orderDate = orderRequest.orderDate,
                deliveryDate = orderRequest.deliveryDate,
                totalAmount = orderRequest.totalAmount,
                totalApprovedAmount = orderRequest.totalApprovedAmount ?: BigDecimal.ZERO,
                orderRequestStatus = orderRequest.orderRequestStatus,
                isClosed = isClosed,
                orderedItemCount = orderedItems.size,
                orderedItems = orderedItems,
                orderProcessingStatusList = orderProcessingStatusList,
                rejectedItems = rejectedItems,
            )
    }
}
