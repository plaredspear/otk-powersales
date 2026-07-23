package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.entity.ErpOrder
import com.otoki.powersales.domain.activity.order.repository.ErpOrderRepository
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.math.BigDecimal
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RelatedOrderQueryService 테스트")
class RelatedOrderQueryServiceTest {

    private val erpOrderRepository: ErpOrderRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val service = RelatedOrderQueryService(erpOrderRepository, employeeRepository)

    init {
        every { employeeRepository.findByEmployeeCodeIn(any()) } returns emptyList()
    }

    @Test
    @DisplayName("정상 - padded/unpadded 양쪽 후보로 IN 조회 (다중 SAP 주문번호 각각 토글)")
    fun buildsBothZeroPaddedCandidatesForAllNumbers() {
        val slot = slot<Collection<String>>()
        every { erpOrderRepository.findByRefSapOrderNumberIn(capture(slot)) } returns emptyList()

        service.findRelatedSummaries(listOf("330884720", "0300011396"))

        // 330884720 → {330884720, 0330884720}, 0300011396 → {0300011396, 300011396}
        assertThat(slot.captured).containsExactlyInAnyOrder(
            "330884720", "0330884720", "0300011396", "300011396",
        )
    }

    @Test
    @DisplayName("정상 - 취소 후속 주문을 요약으로 매핑 + 주문자명 배치 해석")
    fun mapsCancelSummaryWithOrdererName() {
        every { erpOrderRepository.findByRefSapOrderNumberIn(any()) } returns listOf(
            ErpOrder(
                sapOrderNumber = "604311314",
                refSapOrderNumber = "0330884720",
                deliveryRequestDate = LocalDate.of(2026, 7, 22),
                orderDate = LocalDate.of(2026, 7, 21),
                employeeCode = "20180073",
                employeeName = "시스템계정",
                orderSalesAmount = BigDecimal.valueOf(84_000L),
                orderType = "ZRE1",
                orderTypeNm = "Standard Cancel",
            ),
        )
        every { employeeRepository.findByEmployeeCodeIn(listOf("20180073")) } returns
            listOf(Employee(id = 5L, employeeCode = "20180073", name = "황동영"))

        val result = service.findRelatedSummaries(listOf("330884720"))

        assertThat(result).hasSize(1)
        assertThat(result[0].sapOrderNumber).isEqualTo("604311314")
        assertThat(result[0].orderTypeCode).isEqualTo("ZRE1")
        assertThat(result[0].orderTypeName).isEqualTo("Standard Cancel")
        // employee_name(시스템계정) 대신 사번으로 해석한 실명.
        assertThat(result[0].ordererName).isEqualTo("황동영")
        assertThat(result[0].totalApprovedAmount).isEqualByComparingTo(BigDecimal.valueOf(84_000L))
    }

    @Test
    @DisplayName("정상 - 본인 SAP 주문번호(분할/자기참조)는 결과에서 제외")
    fun excludesOwnSapOrderNumbers() {
        every { erpOrderRepository.findByRefSapOrderNumberIn(any()) } returns listOf(
            // 본인 분할 주문이 우연히 ref 로 걸려도 own 집합에 있으면 제외.
            ErpOrder(sapOrderNumber = "330884720", refSapOrderNumber = "0330884721"),
            ErpOrder(sapOrderNumber = "604311314", refSapOrderNumber = "0330884720", orderType = "ZRE1"),
        )

        val result = service.findRelatedSummaries(listOf("330884720", "330884721"))

        assertThat(result.map { it.sapOrderNumber }).containsExactly("604311314")
    }

    @Test
    @DisplayName("정상 - 후속 주문 없으면 빈 배열 (조회 스킵)")
    fun emptyWhenNoOwnNumbers() {
        val result = service.findRelatedSummaries(emptyList())

        assertThat(result).isEmpty()
    }
}
