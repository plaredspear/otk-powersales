package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct

interface OrderRequestProductRepositoryCustom {

    /**
     * 주문요청의 라인 목록 — 라인번호 오름차순. 응답/SAP 페이로드 변환이 product (LAZY) 를 행마다 읽으므로
     * product 를 fetch join 하여 N+1 회피.
     */
    fun findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId: Long): List<OrderRequestProduct>
}
