package com.otoki.powersales.domain.activity.order.dto.request

/**
 * 주문 취소 요청 DTO (Spec #597).
 *
 * `orderProductIds` 가 빈 배열이면 해당 주문의 모든 라인을 취소 (전체 취소).
 * 일부 라인 PK 만 포함되면 부분 취소 (레거시 `view.jsp:131-140` 동등).
 */
data class OrderCancelRequest(
    val orderProductIds: List<Long> = emptyList(),
)
