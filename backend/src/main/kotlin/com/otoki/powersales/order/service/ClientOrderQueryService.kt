package com.otoki.powersales.order.service

import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.order.dto.response.ClientOrderDetailResponse
import com.otoki.powersales.order.dto.response.OrderHistoryGroupResponse
import com.otoki.powersales.order.dto.response.OrderHistoryProductResponse
import com.otoki.powersales.order.exception.ClientOrderForbiddenException
import com.otoki.powersales.order.exception.InvalidSapOrderNumberException
import com.otoki.powersales.order.exception.SapOrderNotFoundException
import com.otoki.powersales.order.repository.ErpOrderProductRepository
import com.otoki.powersales.order.repository.ErpOrderRepository
import java.time.LocalDate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 거래처별 출하 주문 상세 조회 Service (Spec #593).
 *
 * SAP 인바운드(#561)가 적재한 `erp_order` / `erp_order_product` 데이터를 모바일에 노출하는 read-only 서비스.
 * SF 거치지 않는 직접 조회.
 *
 * 권한 게이트: `erp_order.employee_code == JWT 사용자 사번` 일치 확인 (불일치 시 403).
 */
@Service
@Transactional(readOnly = true)
class ClientOrderQueryService(
    private val erpOrderRepository: ErpOrderRepository,
    private val erpOrderProductRepository: ErpOrderProductRepository,
    private val employeeRepository: EmployeeRepository
) {

    companion object {
        private val SAP_ORDER_NUMBER_PATTERN = Regex("^\\d{1,20}$")
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

    /**
     * 거래처 주문이력(제품 선택용) 조회 — 레거시 SF `OrderHistory` 정합.
     *
     * 요청 사용자의 사번으로 게이트하여, 해당 거래처에 본인이 등록한 주문의 제품을 주문일별로 그룹핑한다.
     *
     * @param userId JWT 사용자 ID
     * @param accountCode 거래처 SAP 코드 (erp_order.sap_account_code)
     * @param startDate 주문일 시작
     * @param endDate 주문일 종료
     * @return 주문일 내림차순 그룹 목록 (각 그룹 내 제품은 제품코드 기준 중복제거)
     */
    fun getAccountOrderHistory(
        userId: Long,
        accountCode: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<OrderHistoryGroupResponse> {
        val requesterEmployeeCode = employeeRepository.findById(userId)
            .map { it.employeeCode }
            .orElseThrow { ClientOrderForbiddenException() }

        if (requesterEmployeeCode.isNullOrBlank()) {
            throw ClientOrderForbiddenException()
        }

        val rows = erpOrderProductRepository.findOrderHistory(
            accountCode = accountCode,
            employeeCode = requesterEmployeeCode,
            startDate = startDate,
            endDate = endDate
        )

        return rows
            .filter { it.orderDate != null && it.productCode != null }
            .groupBy { it.orderDate!! }
            .toSortedMap(reverseOrder())
            .map { (date, groupRows) ->
                val products = groupRows
                    .distinctBy { it.productCode }
                    .map { OrderHistoryProductResponse(it.productCode!!, it.productName) }
                OrderHistoryGroupResponse(orderDate = date.toString(), products = products)
            }
    }
}
