package com.otoki.powersales.domain.activity.order.dto.response

import com.otoki.powersales.domain.foundation.product.dto.response.OrderProductDto

/**
 * 거래처 주문이력(제품 선택용) 응답 — 레거시 SF `OrderHistory`(IF_REST_MOBILE_OrderHistory) 정합.
 *
 * 데이터 소스는 레거시와 동일하게 주문요청(`order_request` / `order_request_product`,
 * SF `DKRetail__OrderRequest__c`)이다. 주문일(orderDate) 별로 제품을 그룹핑하며(레거시 `productGrp`),
 * 제품추가 팝업의 "주문 이력" 탭에서 날짜 그룹 헤더 + 제품 목록으로 표시한다.
 *
 * 각 제품은 제품검색/즐겨찾기 탭과 동일한 [OrderProductDto] 형상(바코드·단가·박스입수·차단룰 포함)으로
 * 내려, 이력에서 바로 주문 라인으로 담을 수 있게 한다(레거시는 ProductCode 로 제품정보를 재조회).
 */
data class OrderHistoryGroupResponse(
    /** 주문일 (YYYY-MM-DD). */
    val orderDate: String,
    val products: List<OrderProductDto>
)
