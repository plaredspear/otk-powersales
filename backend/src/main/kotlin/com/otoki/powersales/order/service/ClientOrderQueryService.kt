package com.otoki.powersales.order.service

import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.order.dto.response.ClientOrderDetailResponse
import com.otoki.powersales.order.dto.response.ClientOrderSummaryResponse
import com.otoki.powersales.order.exception.ClientNotFoundException
import com.otoki.powersales.order.exception.ClientOrderForbiddenException
import com.otoki.powersales.order.exception.InvalidOrderParameterException
import com.otoki.powersales.order.exception.InvalidSapOrderNumberException
import com.otoki.powersales.order.exception.SapOrderNotFoundException
import com.otoki.powersales.order.repository.ErpOrderProductRepository
import com.otoki.powersales.order.repository.ErpOrderRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 거래처별 출하 주문 조회 Service (Spec #593).
 *
 * SAP 인바운드(#561)가 적재한 `erp_order` / `erp_order_product` 데이터를 모바일에 노출하는 read-only 서비스.
 * SF 거치지 않는 직접 조회.
 *
 * - 목록 조회(`getClientOrders`): 레거시 `ClientOrderSearch` 와 동등. 거래처 단위 (담당자 필터 없음).
 * - 상세 조회(`getClientOrderDetail`): 권한 게이트 `erp_order.employee_code == JWT 사용자 사번` (불일치 시 403).
 */
@Service
@Transactional(readOnly = true)
class ClientOrderQueryService(
    private val erpOrderRepository: ErpOrderRepository,
    private val erpOrderProductRepository: ErpOrderProductRepository,
    private val employeeRepository: EmployeeRepository,
    private val accountRepository: AccountRepository
) {

    companion object {
        private val SAP_ORDER_NUMBER_PATTERN = Regex("^\\d{1,20}$")
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 100
    }

    /**
     * 거래처별 주문 목록 조회 (거래처별 주문 탭).
     *
     * @param clientId 거래처 ID (필수)
     * @param deliveryDate 납기일 (null 이면 전체)
     * @param page 페이지 번호 (기본 0)
     * @param size 페이지 크기 (기본 20, 최대 100)
     */
    fun getClientOrders(
        clientId: Long,
        deliveryDate: LocalDate?,
        page: Int?,
        size: Int?
    ): Page<ClientOrderSummaryResponse> {
        val resolvedPage = page ?: 0
        val resolvedSize = size ?: DEFAULT_PAGE_SIZE
        validatePagination(resolvedPage, resolvedSize)

        if (!accountRepository.existsById(clientId)) {
            throw ClientNotFoundException()
        }

        // 최근 납기일 → 최신 주문번호 순 정렬 (레거시 ClientOrderSearch 와 동등한 기본 정렬)
        val pageable = PageRequest.of(
            resolvedPage,
            resolvedSize,
            Sort.by(Sort.Direction.DESC, "deliveryRequestDate", "sapOrderNumber")
        )

        return erpOrderRepository.findClientOrders(clientId, deliveryDate, pageable)
            .map(ClientOrderSummaryResponse::from)
    }

    fun getClientOrderDetail(userId: Long, sapOrderNumber: String): ClientOrderDetailResponse {
        if (!SAP_ORDER_NUMBER_PATTERN.matches(sapOrderNumber)) {
            throw InvalidSapOrderNumberException()
        }

        val order = erpOrderRepository.findBySapOrderNumber(sapOrderNumber)
            ?: throw SapOrderNotFoundException()

        val requesterEmployeeCode = employeeRepository.findById(userId)
            .map { it.employeeCode }
            .orElseThrow { ClientOrderForbiddenException() }

        if (order.employeeCode.isNullOrBlank() || order.employeeCode != requesterEmployeeCode) {
            throw ClientOrderForbiddenException()
        }

        val products = erpOrderProductRepository.findBySapOrderNumberOrderByLineNumberAsc(sapOrderNumber)
        return ClientOrderDetailResponse.from(order, products)
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
