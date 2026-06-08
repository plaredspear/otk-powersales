package com.otoki.powersales.order.repository

import com.otoki.powersales.order.entity.OrderRequest
import com.otoki.powersales.order.enums.OrderRequestStatus
import java.time.LocalDate
import java.time.LocalDateTime

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

    /**
     * 거래처 주문이력(제품 선택용) — 레거시 SF `OrderHistory`(IF_REST_MOBILE_OrderHistory) 정합.
     *
     * 본인(employeeId) 이 해당 거래처(account.externalKey = accountCode, 레거시 ExternalKey__c)에
     * 등록한 주문요청의 제품을 주문일(orderDate) 범위로 조회한다. 그룹핑/중복제거는 서비스에서 수행.
     *
     * @param orderDateFrom 주문일 시작 (inclusive)
     * @param orderDateToExclusive 주문일 종료 (exclusive — 레거시 EndDate +1일 처리 대응)
     */
    fun findOrderHistory(
        employeeId: Long,
        accountCode: String,
        orderDateFrom: LocalDateTime,
        orderDateToExclusive: LocalDateTime,
    ): List<OrderHistoryRow>
}

/** 거래처 주문이력 조회 행 (주문일시 + 제품). */
data class OrderHistoryRow(
    val orderDate: LocalDateTime?,
    val productCode: String?,
    val productName: String?,
)
