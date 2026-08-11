package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.foundation.product.entity.Product
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
     * 본인(employeeId) 이 해당 거래처(account.id = accountId)에 등록한 주문요청의 제품을
     * 주문일(orderDate) 범위로 조회한다. 그룹핑/중복제거는 서비스에서 수행.
     * (거래처 식별을 SAP externalKey 가 아닌 내부 ID 로 하여, SAP 미연동 거래처도 조회 가능.)
     *
     * @param orderDateFrom 주문일 시작 (inclusive)
     * @param orderDateToExclusive 주문일 종료 (exclusive — 레거시 EndDate +1일 처리 대응)
     */
    fun findOrderHistory(
        employeeId: Long,
        accountId: Long,
        orderDateFrom: LocalDateTime,
        orderDateToExclusive: LocalDateTime,
    ): List<OrderHistoryRow>

    /**
     * 최근 주문 제품 ID 목록 — 제품검색 결과 상단 정렬용.
     *
     * 본인(employeeId)이 [orderDateFrom] 이후 주문한 제품을 최신 주문순으로 [limit] 개까지
     * 반환한다. 거래처 조건은 걸지 않는다(주문서에서 거래처 미선택 상태로도 제품검색이 열림 —
     * 거래처 AND 인 [findOrderHistory] 와 기준이 다르다).
     *
     * 정렬용 IN 절 파라미터로 쓰이므로 [limit] 로 크기를 제한한다. 주문 상태는 구분하지 않고
     * 삭제분만 제외한다([findOrderHistory] 와 동일).
     */
    fun findRecentlyOrderedProductIds(
        employeeId: Long,
        orderDateFrom: LocalDateTime,
        limit: Int,
    ): List<Long>
}

/**
 * 거래처 주문이력 조회 행 (주문일시 + 제품 마스터 + 단위매칭 바코드).
 *
 * 제품검색/즐겨찾기 탭과 동일한 [com.otoki.powersales.domain.foundation.product.dto.response.OrderProductDto]
 * 로 변환하기 위해 제품 엔티티 전체와 단위매칭 바코드를 함께 담는다.
 */
data class OrderHistoryRow(
    val orderDate: LocalDateTime?,
    val product: Product?,
    val barcode: String?,
)
