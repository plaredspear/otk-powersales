package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.order.service.ErpOrderUpsertService
import com.otoki.powersales.order.service.dto.ErpOrderUpsertCommand
import com.otoki.powersales.order.service.dto.ErpOrderUpsertFailedRow
import com.otoki.powersales.order.service.dto.ErpOrderUpsertResult
import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.inbound.dto.order.ErpOrderItemDetail
import com.otoki.powersales.sap.inbound.dto.order.ErpOrderRequestItem
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("SapErpOrderService 어댑터 테스트")
class SapErpOrderServiceTest {

    @Mock
    private lateinit var erpOrderUpsertService: ErpOrderUpsertService

    @Mock
    private lateinit var auditService: SapInboundAuditService

    @InjectMocks
    private lateinit var service: SapErpOrderService

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
        @DisplayName("happy: 도메인 결과 (header=1, line=1, failures=0) → ErpOrderDetail successCount=1 + audit")
        fun happy_domainResultMappedAndAudit() {
            whenever(erpOrderUpsertService.upsert(any())).thenReturn(
                ErpOrderUpsertResult(headerSuccessCount = 1, lineSuccessCount = 1, failures = emptyList())
            )

            val detail = service.upsert(listOf(header()))

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(0)

            val auditCaptor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(auditCaptor.capture())
            assertThat(auditCaptor.firstValue.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_ACCEPTED)
            assertThat(auditCaptor.firstValue.reason).isEqualTo("success=1 failure=0")
        }

        @Test
        @DisplayName("부분 실패: 도메인 failures → ErpOrderFailure 매핑 (sapOrderNumber 보존)")
        fun partialFailure_failureRowsMapped() {
            whenever(erpOrderUpsertService.upsert(any())).thenReturn(
                ErpOrderUpsertResult(
                    headerSuccessCount = 1,
                    lineSuccessCount = 1,
                    failures = listOf(ErpOrderUpsertFailedRow("0010000002", "account not found"))
                )
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
        @DisplayName("도메인 throw (라인 ConstraintViolation 시뮬): 실패 audit 후 예외 재전파")
        fun domainThrow_failureAuditAndRethrow() {
            whenever(erpOrderUpsertService.upsert(any()))
                .thenThrow(IllegalStateException("constraint violation"))

            assertThatThrownBy { service.upsert(listOf(header())) }
                .isInstanceOf(IllegalStateException::class.java)

            val auditCaptor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(auditCaptor.capture())
            assertThat(auditCaptor.firstValue.reason).isEqualTo("success=0 failure=1")
        }

        @Test
        @DisplayName("DTO 매핑: ErpOrderRequestItem (헤더 + ItemDetailList) → ErpOrderUpsertCommand (헤더 + lines)")
        fun dtoMapping_itemToCommand() {
            whenever(erpOrderUpsertService.upsert(any())).thenReturn(
                ErpOrderUpsertResult(headerSuccessCount = 1, lineSuccessCount = 1, failures = emptyList())
            )

            service.upsert(listOf(header(lines = listOf(line(lineNumber = "001"), line(lineNumber = "002")))))

            val captor = argumentCaptor<List<ErpOrderUpsertCommand>>()
            verify(erpOrderUpsertService).upsert(captor.capture())
            val command = captor.firstValue.single()
            assertThat(command.sapOrderNumber).isEqualTo("0010012345")
            assertThat(command.sapAccountCode).isEqualTo("1032619")
            assertThat(command.orderSalesAmount).isEqualTo("1500000")
            assertThat(command.lines).hasSize(2)
            assertThat(command.lines.map { it.lineNumber }).containsExactly("001", "002")
        }
    }
}
