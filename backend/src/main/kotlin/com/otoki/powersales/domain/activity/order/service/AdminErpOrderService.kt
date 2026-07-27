package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.dto.response.AdminErpOrderDetailResponse
import com.otoki.powersales.domain.activity.order.dto.response.AdminErpOrderListItem
import com.otoki.powersales.domain.activity.order.dto.response.AdminErpOrderListResponse
import com.otoki.powersales.domain.activity.order.exception.ErpOrderNotFoundException
import com.otoki.powersales.domain.activity.order.exception.InvalidOrderParameterException
import com.otoki.powersales.domain.activity.order.repository.ErpOrderProductRepository
import com.otoki.powersales.domain.activity.order.repository.ErpOrderRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 관리자 웹 ERP주문 조회 Service (기준정보 > ERP주문 조회 화면).
 *
 * SAP 인바운드(#561)가 적재한 `erp_order` / `erp_order_product` 데이터를 web admin 에 노출하는 read-only 서비스.
 * 거래처별 모바일 조회([ClientOrderQueryService])와 달리 거래처 제약 없이 전체 ERP주문을 대상으로
 * 주문번호 정확일치 / 납기일 기간 필터로 조회한다.
 */
@Service
@Transactional(readOnly = true)
class AdminErpOrderService(
    private val erpOrderRepository: ErpOrderRepository,
    private val erpOrderProductRepository: ErpOrderProductRepository
) {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 100
    }

    /**
     * ERP주문 목록 조회 — 최신 주문(id DESC) 순 페이징.
     */
    fun getErpOrders(
        sapOrderNumber: String?,
        deliveryDateFrom: LocalDate?,
        deliveryDateTo: LocalDate?,
        page: Int?,
        size: Int?
    ): AdminErpOrderListResponse {
        val resolvedPage = page ?: 0
        val resolvedSize = size ?: DEFAULT_PAGE_SIZE
        validatePagination(resolvedPage, resolvedSize)

        val pageable = PageRequest.of(resolvedPage, resolvedSize)
        val orders = erpOrderRepository.findAdminErpOrders(
            sapOrderNumber = sapOrderNumber,
            deliveryDateFrom = deliveryDateFrom,
            deliveryDateTo = deliveryDateTo,
            pageable = pageable
        )

        return AdminErpOrderListResponse(
            content = orders.content.map { AdminErpOrderListItem.from(it) },
            page = orders.number,
            size = orders.size,
            totalElements = orders.totalElements,
            totalPages = orders.totalPages
        )
    }

    /**
     * ERP주문 상세 조회 — 헤더 + 주문상품 라인(lineNumber 오름차순).
     *
     * 거래처별 모바일 상세([ClientOrderQueryService.getClientOrderDetail])의 역참조 통합/제품 dedup 로직은
     * 적용하지 않는다 — 관리자 화면은 적재된 원본 라인을 그대로 노출한다.
     */
    fun getErpOrderDetail(id: Long): AdminErpOrderDetailResponse {
        val order = erpOrderRepository.findById(id).orElseThrow { ErpOrderNotFoundException() }
        val products = erpOrderProductRepository.findBySapOrderNumberOrderByLineNumberAsc(order.sapOrderNumber)
        return AdminErpOrderDetailResponse.from(order, products)
    }

    private fun validatePagination(page: Int, size: Int) {
        if (page < 0) {
            throw InvalidOrderParameterException("페이지 번호는 0 이상이어야 합니다")
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw InvalidOrderParameterException("페이지 크기는 1~$MAX_PAGE_SIZE 범위여야 합니다")
        }
    }
}
