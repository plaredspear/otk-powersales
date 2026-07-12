package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.dto.response.ClientOrderDetailResponse
import com.otoki.powersales.domain.activity.order.dto.response.ClientOrderSummaryResponse
import com.otoki.powersales.domain.activity.order.exception.ClientNotFoundException
import com.otoki.powersales.domain.activity.order.exception.InvalidOrderParameterException
import com.otoki.powersales.domain.activity.order.exception.InvalidSapOrderNumberException
import com.otoki.powersales.domain.activity.order.exception.SapOrderNotFoundException
import com.otoki.powersales.domain.activity.order.repository.ErpOrderProductRepository
import com.otoki.powersales.domain.activity.order.repository.ErpOrderRepository
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

/**
 * 거래처별 출하 주문 조회 Service (Spec #593).
 *
 * SAP 인바운드(#561)가 적재한 `erp_order` / `erp_order_product` 데이터를 모바일에 노출하는 read-only 서비스.
 * SF 거치지 않는 직접 조회.
 *
 * - 목록 조회(`getClientOrders`): 레거시 `ClientOrderSearch` 와 동등. 거래처 단위 (담당자 필터 없음).
 * - 상세 조회(`getClientOrderDetail`): 레거시 `IF_REST_MOBILE_ClientOrderDetail` 와 동등. SAP 주문번호 단독 조회
 *   (담당자/사번 권한 게이트 없음 — 레거시 데이터 조회 결과 정합).
 */
@Service
@Transactional(readOnly = true)
class ClientOrderQueryService(
    private val erpOrderRepository: ErpOrderRepository,
    private val erpOrderProductRepository: ErpOrderProductRepository,
    private val accountRepository: AccountRepository,
    private val employeeRepository: EmployeeRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {

    companion object {
        private val SAP_ORDER_NUMBER_PATTERN = Regex("^\\d{1,20}$")
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 100
    }

    /**
     * 거래처별 주문 목록 조회 (거래처별 주문 탭).
     *
     * 레거시 `ClientOrderSearch`(`DeliveryRequestDate__c =: 단일 날짜`) 와 동등하게 **납기일 단일 날짜**로 조회한다.
     * 레거시 화면이 납기일을 항상 전송(기본 오늘)하므로, null 입력 시 오늘로 기본 적용한다 (전체 조회 아님 — 레거시 데이터 조회 결과 정합).
     *
     * @param userId 로그인 사용자 ID (내 주문 강조용 `isMine` 판정)
     * @param clientId 거래처 ID (필수)
     * @param deliveryDate 납기일 (null 이면 오늘 기준)
     * @param page 페이지 번호 (기본 0)
     * @param size 페이지 크기 (기본 20, 최대 100)
     */
    fun getClientOrders(
        userId: Long,
        clientId: Long,
        deliveryDate: LocalDate?,
        page: Int?,
        size: Int?
    ): Page<ClientOrderSummaryResponse> {
        val resolvedPage = page ?: 0
        val resolvedSize = size ?: DEFAULT_PAGE_SIZE
        validatePagination(resolvedPage, resolvedSize)
        val resolvedDeliveryDate = deliveryDate ?: LocalDate.now(clock)

        if (!accountRepository.existsById(clientId)) {
            throw ClientNotFoundException()
        }

        // 로그인 사원 사번 — 각 주문의 주문자사번과 비교해 "내 주문"(isMine) 판정. 미존재 시 전부 false.
        val currentEmployeeCode = employeeRepository.findById(userId).orElse(null)?.employeeCode

        // 최근 납기일 → 최신 주문번호 순 정렬 (레거시 ClientOrderSearch 와 동등한 기본 정렬)
        val pageable = PageRequest.of(
            resolvedPage,
            resolvedSize,
            Sort.by(Sort.Direction.DESC, "deliveryRequestDate", "sapOrderNumber")
        )

        return erpOrderRepository.findClientOrders(clientId, resolvedDeliveryDate, pageable)
            .map { ClientOrderSummaryResponse.from(it, currentEmployeeCode) }
    }

    /**
     * 거래처별 주문 상세 조회 (SAP 주문번호 단독).
     *
     * 레거시 `IF_REST_MOBILE_ClientOrderDetail`(`WHERE name = :SAPOrderNumber LIMIT 1`) 와 동일하게
     * 담당자/사번 권한 게이트 없이 주문번호로만 조회한다 (레거시 데이터 조회 결과 정합).
     */
    fun getClientOrderDetail(sapOrderNumber: String): ClientOrderDetailResponse {
        if (!SAP_ORDER_NUMBER_PATTERN.matches(sapOrderNumber)) {
            throw InvalidSapOrderNumberException()
        }

        val order = erpOrderRepository.findBySapOrderNumber(sapOrderNumber)
            ?: throw SapOrderNotFoundException()

        val products = erpOrderProductRepository.findBySapOrderNumberOrderByLineNumberAsc(sapOrderNumber)

        // 주문자명은 erp_order.employee_name(시스템 계정명 오적재 가능) 대신 사번으로 Employee 마스터에서 해석.
        // 단건 상세라 조회 1회 — N+1 아님. 미해석 시 기존 employee_name 로 폴백.
        val ordererName = order.employeeCode
            ?.takeIf { it.isNotBlank() }
            ?.let { employeeRepository.findByEmployeeCode(it).orElse(null)?.name }
            ?: order.employeeName

        return ClientOrderDetailResponse.from(order, products, ordererName)
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
