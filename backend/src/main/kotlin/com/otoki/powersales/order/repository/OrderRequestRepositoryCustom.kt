package com.otoki.powersales.order.repository

import com.otoki.powersales.order.entity.OrderRequest
import com.otoki.powersales.order.entity.OrderRequestStatus
import java.time.LocalDate

/**
 * 본인 주문요청 조회용 동적 필터 + 정렬 (페이징 없음 — 클라이언트 슬라이스 정책).
 */
interface OrderRequestRepositoryCustom {

    fun findMyOrderRequests(
        employeeId: Long,
        accountId: Long?,
        status: OrderRequestStatus?,
        deliveryDateFrom: LocalDate,
        deliveryDateTo: LocalDate,
        sortBy: String,
        sortDir: String,
        limit: Int,
    ): List<OrderRequest>
}
