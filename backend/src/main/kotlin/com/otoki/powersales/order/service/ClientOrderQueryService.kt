package com.otoki.powersales.order.service

import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.order.dto.response.ClientOrderDetailResponse
import com.otoki.powersales.order.exception.ClientOrderForbiddenException
import com.otoki.powersales.order.exception.InvalidSapOrderNumberException
import com.otoki.powersales.order.exception.SapOrderNotFoundException
import com.otoki.powersales.order.repository.ErpOrderProductRepository
import com.otoki.powersales.order.repository.ErpOrderRepository
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
}
