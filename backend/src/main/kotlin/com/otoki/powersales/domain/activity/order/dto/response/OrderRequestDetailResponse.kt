package com.otoki.powersales.domain.activity.order.dto.response

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
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
    // orderDate / deliveryDate / totalAmount / orderRequestStatus 는 SF nillable=true 정합으로 nullable (마이그 SF NULL row 보존).
    val orderDate: LocalDateTime?,
    val deliveryDate: LocalDate?,
    val totalAmount: BigDecimal?,
    // 총 승인 금액 — SAP OrderRequestDetail 응답 전 라인 OrderSalesAmount 합산 (레거시 view.jsp:343-348
    // 동등). DB order_request.total_approved_amount 컬럼이 아니라 조회 시점 SAP 실측치. SAP 실패 시 0.
    val totalApprovedAmount: BigDecimal,
    // 상태는 코드(영문)와 한글 표시명을 분리해 내려준다 (목록 API 와 정합). 모바일은 표시명을 그대로 출력하고
    // 코드는 색상/분기 로직에만 사용한다 (클라이언트 enum 매핑 제거).
    // SF nillable=true 정합으로 orderRequestStatus 가 nullable — 마이그 SF NULL row 는 두 필드 모두 null.
    val orderRequestStatus: String?,
    val orderRequestStatusName: String?,
    val isClosed: Boolean,
    // 취소 가능 여부(서버 권위 판정: 상태 + 마감 + 등록 SAP 전송 not in-flight). 모바일 취소 버튼 게이트 단일 진실원.
    val cancelable: Boolean,
    // 등록 SAP 전송이 아직 진행 중(outbox PENDING/RETRY)인지 — 취소 불가 사유가 "전송 처리 중"임을 안내하기 위함.
    val registrationInFlight: Boolean,
    val orderedItemCount: Int,
    val orderedItems: List<OrderedItemResponse>,
    val orderProcessingStatusList: List<OrderProcessingStatusResponse>?,
    val rejectedItems: List<RejectedItemResponse>?,
) {
    companion object {
        fun of(
            orderRequest: OrderRequest,
            isClosed: Boolean,
            cancelable: Boolean,
            registrationInFlight: Boolean,
            totalApprovedAmount: BigDecimal,
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
                totalApprovedAmount = totalApprovedAmount,
                orderRequestStatus = orderRequest.orderRequestStatus?.name,
                orderRequestStatusName = orderRequest.orderRequestStatus?.displayName,
                isClosed = isClosed,
                cancelable = cancelable,
                registrationInFlight = registrationInFlight,
                orderedItemCount = orderedItems.size,
                orderedItems = orderedItems,
                orderProcessingStatusList = orderProcessingStatusList,
                rejectedItems = rejectedItems,
            )
    }
}
