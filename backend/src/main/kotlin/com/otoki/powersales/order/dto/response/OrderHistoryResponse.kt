package com.otoki.powersales.order.dto.response

/**
 * 거래처 주문이력(제품 선택용) 응답 — 레거시 SF `OrderHistory`(IF_REST_MOBILE_OrderHistory) 정합.
 *
 * 데이터 소스는 레거시와 동일하게 주문요청(`order_request` / `order_request_product`,
 * SF `DKRetail__OrderRequest__c`)이다. 주문일(orderDate) 별로 제품을 그룹핑하며(레거시 `productGrp`),
 * 제품추가 팝업의 "주문 이력" 탭에서 날짜 그룹 헤더 + 제품 목록(제품명/제품코드)으로 표시한다.
 */
data class OrderHistoryGroupResponse(
    /** 주문일 (YYYY-MM-DD). */
    val orderDate: String,
    val products: List<OrderHistoryProductResponse>
)

data class OrderHistoryProductResponse(
    val productCode: String,
    val productName: String?
)
