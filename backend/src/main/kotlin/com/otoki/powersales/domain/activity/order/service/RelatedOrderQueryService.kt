package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.dto.response.RelatedClientOrderResponse
import com.otoki.powersales.domain.activity.order.repository.ErpOrderRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 특정 SAP 주문번호를 `ref_sap_order_number` 로 역참조하는 후속 주문(취소/변경 등) **요약** 조회 헬퍼.
 *
 * 거래처별 주문 상세와 내 주문 상세가 공유한다.
 * - 거래처별([ClientOrderQueryService])은 단일 SAP 주문번호 상세라 자체 로직에서 후속 주문 엔티티를 직접 다뤄
 *   제품 통합 dedup 까지 수행하므로 이 헬퍼를 쓰지 않는다.
 * - 내 주문 상세([OrderRequestService])는 하나의 주문요청이 다중 SAP 주문으로 분할될 수 있어, 그 distinct
 *   SAP 주문번호 목록 전체를 대상으로 후속 주문을 **요약 섹션**으로만 노출한다(제품 목록 통합은 데이터 모델
 *   차이로 비범위).
 *
 * SAP 은 원본 주문번호를 선행 0 한 자리로 zero-pad 하여 참조 필드에 적재하므로(예: 원본 `330884720`
 * → 취소건 `ref = 0330884720`), padded/unpadded 양쪽 후보([buildRefCandidates])로 매칭한다.
 */
@Service
@Transactional(readOnly = true)
class RelatedOrderQueryService(
    private val erpOrderRepository: ErpOrderRepository,
    private val employeeRepository: EmployeeRepository,
) {

    /**
     * [ownSapOrderNumbers] 를 `ref_sap_order_number` 로 역참조하는 후속 주문 요약 목록.
     *
     * 자기 자신([ownSapOrderNumbers] 에 포함된 주문번호)은 제외한다. 주문번호별 1건으로 축약하고
     * 주문일/주문번호 오름차순 정렬한다. 주문자명은 후속 주문 전체 사번을 모아 `findByEmployeeCodeIn` 1회로
     * 배치 해석한다(N+1 방지). 미해석 시 `employee_name` 폴백.
     */
    fun findRelatedSummaries(ownSapOrderNumbers: Collection<String>): List<RelatedClientOrderResponse> {
        val own = ownSapOrderNumbers.filter { it.isNotBlank() }.toSet()
        if (own.isEmpty()) return emptyList()

        val candidates = own.flatMapTo(mutableSetOf()) { buildRefCandidates(it) }
        val related = erpOrderRepository.findByRefSapOrderNumberIn(candidates)
            .filter { it.sapOrderNumber !in own } // 본인 분할 주문/자기참조 제외
            .distinctBy { it.sapOrderNumber }
            .sortedWith(compareBy({ it.orderDate }, { it.sapOrderNumber }))
        if (related.isEmpty()) return emptyList()

        val ordererNameByCode: Map<String, String> = related
            .mapNotNull { it.employeeCode?.takeIf(String::isNotBlank) }
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?.let { codes ->
                employeeRepository.findByEmployeeCodeIn(codes)
                    .mapNotNull { emp -> emp.employeeCode?.let { it to emp.name } }
                    .toMap()
            }
            ?: emptyMap()

        return related.map { relatedOrder ->
            val ordererName = relatedOrder.employeeCode?.let(ordererNameByCode::get) ?: relatedOrder.employeeName
            RelatedClientOrderResponse.from(relatedOrder, ordererName)
        }
    }

    /**
     * 역참조 조회용 후보 주문번호 집합 — 선행 0 한 자리 유무 양쪽.
     * `330884720` → `{330884720, 0330884720}`, `0300011396` → `{0300011396, 300011396}`.
     */
    private fun buildRefCandidates(sapOrderNumber: String): Set<String> = buildSet {
        add(sapOrderNumber)
        if (sapOrderNumber.startsWith("0")) add(sapOrderNumber.substring(1)) else add("0$sapOrderNumber")
    }
}
