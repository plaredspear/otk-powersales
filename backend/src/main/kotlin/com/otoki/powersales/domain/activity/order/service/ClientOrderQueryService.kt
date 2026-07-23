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

        /** 취소 주문유형 코드 (SAP `Standard Cancel`). */
        private const val CANCEL_ORDER_TYPE = "ZRE1"
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

        val orders = erpOrderRepository.findClientOrders(clientId, resolvedDeliveryDate, pageable)

        // 주문자명은 employee_name(시스템 계정명 오적재 가능) 대신 사번으로 해석.
        // 페이지 내 사번을 모아 findByEmployeeCodeIn 1회로 조회 → map 재주입 (N+1 방지). 미해석 시 employee_name 폴백.
        val ordererNameByCode: Map<String, String> = orders.content
            .mapNotNull { it.employeeCode?.takeIf(String::isNotBlank) }
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?.let { codes ->
                employeeRepository.findByEmployeeCodeIn(codes)
                    .mapNotNull { emp -> emp.employeeCode?.let { it to emp.name } }
                    .toMap()
            }
            ?: emptyMap()

        return orders.map {
            val ordererName = it.employeeCode?.let(ordererNameByCode::get) ?: it.employeeName
            ClientOrderSummaryResponse.from(it, currentEmployeeCode, ordererName)
        }
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

        // 이 주문번호를 `ref_sap_order_number` 로 역참조하는 후속 주문(취소/변경 등) (2026-07-23 사용자 요청).
        // SAP 은 원본 주문번호를 선행 0 한 자리로 zero-pad 하여 참조 필드에 적재하므로(예: 원본 `330884720`
        // → 취소건 `ref = 0330884720`), padded/unpadded 양쪽 후보([buildRefCandidates])로 매칭한다.
        // 원본 자기 자신은 제외.
        val referencingOrders = erpOrderRepository.findByRefSapOrderNumberIn(buildRefCandidates(sapOrderNumber))
            .filter { it.sapOrderNumber != sapOrderNumber }
            .sortedWith(compareBy({ it.orderDate }, { it.sapOrderNumber }))

        // 제품 목록 = 원주문 제품 + 후속주문 제품을 통합해 제품코드 기준 최신(최대 id) 1건으로 dedup (2026-07-23 사용자 결정).
        // 후속주문(취소)의 동일 제품 레코드가 원주문 레코드보다 나중(큰 id)이면 그것이 최신 상태로 노출된다(예: '취소').
        // 표시 순서는 원주문 라인(lineNumber 오름차순)에서 제품이 처음 등장한 위치를 유지하고,
        // 후속주문에만 있는 제품은 그 뒤로 붙는다.
        val originalProducts = erpOrderProductRepository.findBySapOrderNumberOrderByLineNumberAsc(sapOrderNumber)
        val referencingProducts =
            if (referencingOrders.isEmpty()) emptyList()
            else erpOrderProductRepository
                .findBySapOrderNumberIn(referencingOrders.map { it.sapOrderNumber })
                .sortedWith(compareBy({ it.sapOrderNumber }, { it.lineNumber }))
        val products = dedupLatestByProduct(originalProducts + referencingProducts)

        // 주문자명은 erp_order.employee_name(시스템 계정명 오적재 가능) 대신 사번으로 Employee 마스터에서 해석.
        // 단건 상세라 조회 1회 — N+1 아님. 미해석 시 기존 employee_name 로 폴백.
        val ordererName = order.employeeCode
            ?.takeIf { it.isNotBlank() }
            ?.let { employeeRepository.findByEmployeeCode(it).orElse(null)?.name }
            ?: order.employeeName

        val relatedOrders = buildRelatedSummaries(referencingOrders)

        // 취소 주문(order_type=ZRE1)의 SAP 주문번호 — 통합 dedup 결과 최신 레코드가 취소 주문 소속이면
        // 라인 상태를 '취소'로 강제(취소는 헤더 order_type 으로 표현되고 라인 delivery_status 엔 미반영).
        val cancelledSapOrderNumbers = referencingOrders
            .filter { isCancelOrderType(it.orderType) }
            .map { it.sapOrderNumber }
            .toSet()

        return ClientOrderDetailResponse.from(
            order, products, ordererName, relatedOrders, cancelledSapOrderNumbers
        )
    }

    /** 취소 주문유형 판정 — SAP `ZRE1`(Standard Cancel). */
    private fun isCancelOrderType(orderType: String?): Boolean = orderType == CANCEL_ORDER_TYPE

    /**
     * 후속 주문 요약(제품표 없음) — 주문번호/유형/일자/금액/주문자만.
     * 제품 라인은 원주문 제품표에 통합 dedup 되므로 요약엔 담지 않는다.
     * 주문자명은 후속 주문 전체 사번을 모아 `findByEmployeeCodeIn` 1회로 배치 해석한다 (N+1 방지).
     */
    private fun buildRelatedSummaries(
        referencingOrders: List<com.otoki.powersales.domain.activity.order.entity.ErpOrder>
    ): List<com.otoki.powersales.domain.activity.order.dto.response.RelatedClientOrderResponse> {
        if (referencingOrders.isEmpty()) return emptyList()

        val ordererNameByCode: Map<String, String> = referencingOrders
            .mapNotNull { it.employeeCode?.takeIf(String::isNotBlank) }
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?.let { codes ->
                employeeRepository.findByEmployeeCodeIn(codes)
                    .mapNotNull { emp -> emp.employeeCode?.let { it to emp.name } }
                    .toMap()
            }
            ?: emptyMap()

        return referencingOrders.map { relatedOrder ->
            val ordererName = relatedOrder.employeeCode?.let(ordererNameByCode::get) ?: relatedOrder.employeeName
            com.otoki.powersales.domain.activity.order.dto.response.RelatedClientOrderResponse.from(
                relatedOrder, ordererName
            )
        }
    }

    /**
     * 역참조 조회용 후보 주문번호 집합 — 선행 0 한 자리 유무 양쪽.
     *
     * `330884720` → `{330884720, 0330884720}`, `0300011396` → `{0300011396, 300011396}`.
     * SAP zero-pad 규격이 `ref_sap_order_number` 에 어느 형태로 적재됐든 매칭하기 위함
     * ([ErpOrderUpsertService.computeExternalKey] 의 선행 0 한 자리 제거 규칙과 동일 축).
     */
    private fun buildRefCandidates(sapOrderNumber: String): Set<String> = buildSet {
        add(sapOrderNumber)
        if (sapOrderNumber.startsWith("0")) add(sapOrderNumber.substring(1)) else add("0$sapOrderNumber")
    }

    /**
     * 같은 제품(productCode)의 다중 라인을 제품별 최신 1건으로 축약한다 (2026-07-23 사용자 결정).
     *
     * SAP 배송분리로 동일 주문라인이 배차 전(대기, 0 BOX)·배차 후(배송중, 실수량)의 **별도 레코드**로 적재된다
     * (externalKey = `주문번호+라인번호(+배송차량)` — 배송차량 유무로 두 레코드가 갈림). 배차 전 레코드는
     * 주문 생성 시점에 만들어져 id 가 작고, 배차 후 레코드는 배차 시점에 새로 적재돼 id 가 크다. 따라서
     * **제품별 최대 id 레코드 = 가장 나중에 적재된 최신 상태**를 남긴다. productCode 가 비어있는 라인은
     * 서로 다른 제품이 뭉개질 위험이 있어 병합하지 않고 각각 유지한다.
     *
     * 참고: 레거시(SF/JSP)는 dedup 없이 모든 라인을 표시했으나(비결정 순서), 본 화면은 사용자 요청으로 제품별
     * 최신 1건만 노출한다. 표시 순서는 원본(lineNumber 오름차순)에서 제품이 처음 등장한 위치를 유지한다.
     */
    private fun dedupLatestByProduct(
        products: List<com.otoki.powersales.domain.activity.order.entity.ErpOrderProduct>
    ): List<com.otoki.powersales.domain.activity.order.entity.ErpOrderProduct> =
        products
            .groupBy { it.productCode?.takeIf(String::isNotBlank) ?: " ${it.id}" }
            .values
            .map { group -> group.maxByOrNull { it.id }!! }

    private fun validatePagination(page: Int, size: Int) {
        if (page < 0) {
            throw InvalidOrderParameterException("페이지 번호는 0 이상이어야 합니다")
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw InvalidOrderParameterException("페이지 크기는 1~$MAX_PAGE_SIZE 범위여야 합니다")
        }
    }
}
