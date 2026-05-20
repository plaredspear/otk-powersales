package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.order.service.ErpOrderUpsertService
import com.otoki.powersales.order.service.dto.ErpOrderUpsertCommand
import com.otoki.powersales.order.service.dto.ErpOrderUpsertFailedRow
import com.otoki.powersales.order.service.dto.ErpOrderUpsertResult
import com.otoki.powersales.sap.inbound.dto.order.ErpOrderItemDetail
import com.otoki.powersales.sap.inbound.dto.order.ErpOrderRequestItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SapErpOrderService 어댑터 테스트")
class SapErpOrderServiceTest {

    private val erpOrderUpsertService: ErpOrderUpsertService = mockk()
    private val service = SapErpOrderService(erpOrderUpsertService)

    private fun line(
        sapOrderNumber: String? = "0010012345",
        lineNumber: String? = "001",
        productCode: String? = "100100"
    ): ErpOrderItemDetail = ErpOrderItemDetail(
        sapOrderNumber = sapOrderNumber,
        lineNumber = lineNumber,
        productCode = productCode,
        productName = "진라면",
        orderQuantity = "100",
        unit = "EA"
    )

    private fun header(
        sapOrderNumber: String? = "0010012345",
        sapAccountCode: String? = "1032619",
        lines: List<ErpOrderItemDetail> = listOf(line())
    ): ErpOrderRequestItem = ErpOrderRequestItem(
        sapOrderNumber = sapOrderNumber,
        sapAccountCode = sapAccountCode,
        sapAccountName = "(주)홍길동상회",
        orderSalesAmount = "1500000",
        orderChannel = "01",
        itemDetailList = lines
    )

    @Nested
    @DisplayName("upsert - 어댑터 책임")
    inner class AdapterResponsibilities {

        @Test
        @DisplayName("happy: 도메인 결과 (header=1, line=1, failures=0) → ErpOrderDetail successCount=1")
        fun happy_domainResultMapped() {
            every { erpOrderUpsertService.upsert(any()) } returns
                ErpOrderUpsertResult(headerSuccessCount = 1, lineSuccessCount = 1, failures = emptyList())

            val detail = service.upsert(listOf(header()))

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(0)
        }

        @Test
        @DisplayName("부분 실패: 도메인 failures → ErpOrderFailure 매핑 (sapOrderNumber 보존)")
        fun partialFailure_failureRowsMapped() {
            every { erpOrderUpsertService.upsert(any()) } returns
                ErpOrderUpsertResult(
                    headerSuccessCount = 1,
                    lineSuccessCount = 1,
                    failures = listOf(ErpOrderUpsertFailedRow("0010000002", "account not found"))
                )

            val detail = service.upsert(
                listOf(
                    header(sapOrderNumber = "0010000001", sapAccountCode = "1032619"),
                    header(sapOrderNumber = "0010000002", sapAccountCode = "9999999")
                )
            )

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().sapOrderNumber).isEqualTo("0010000002")
            assertThat(detail.failures.single().reason).isEqualTo("account not found")
        }

        @Test
        @DisplayName("도메인 throw (라인 ConstraintViolation 시뮬): 어댑터는 catch 하지 않고 그대로 재전파")
        fun domainThrow_propagated() {
            every { erpOrderUpsertService.upsert(any()) } throws
                IllegalStateException("constraint violation")

            assertThatThrownBy { service.upsert(listOf(header())) }
                .isInstanceOf(IllegalStateException::class.java)
        }

        @Test
        @DisplayName("DTO 매핑: ErpOrderRequestItem (헤더 + ItemDetailList) → ErpOrderUpsertCommand (헤더 + lines)")
        fun dtoMapping_itemToCommand() {
            every { erpOrderUpsertService.upsert(any()) } returns
                ErpOrderUpsertResult(headerSuccessCount = 1, lineSuccessCount = 1, failures = emptyList())

            service.upsert(listOf(header(lines = listOf(line(lineNumber = "001"), line(lineNumber = "002")))))

            val captor = slot<List<ErpOrderUpsertCommand>>()
            verify { erpOrderUpsertService.upsert(capture(captor)) }
            val command = captor.captured.single()
            assertThat(command.sapOrderNumber).isEqualTo("0010012345")
            assertThat(command.sapAccountCode).isEqualTo("1032619")
            assertThat(command.orderSalesAmount).isEqualTo("1500000")
            assertThat(command.lines).hasSize(2)
            assertThat(command.lines.map { it.lineNumber }).containsExactly("001", "002")
        }
    }
}
