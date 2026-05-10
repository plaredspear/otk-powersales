package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sales.service.MonthlySalesHistoryUpsertService
import com.otoki.powersales.sales.service.dto.MonthlySalesHistoryUpsertCommand
import com.otoki.powersales.sales.service.dto.MonthlySalesHistoryUpsertFailedRow
import com.otoki.powersales.sales.service.dto.MonthlySalesHistoryUpsertResult
import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.inbound.dto.sales.ChunkResult
import com.otoki.powersales.sap.inbound.dto.sales.MonthlySalesHistoryRequestItem
import com.otoki.powersales.sap.inbound.exception.SapPayloadTooLargeException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("SapMonthlySalesHistoryService 어댑터 테스트")
class SapMonthlySalesHistoryServiceTest {

    @Mock
    private lateinit var monthlySalesHistoryUpsertService: MonthlySalesHistoryUpsertService

    @Mock
    private lateinit var auditService: SapInboundAuditService

    private lateinit var service: SapMonthlySalesHistoryService

    @BeforeEach
    fun setUp() {
        val helper = ChunkedUpsertHelper()
        service = SapMonthlySalesHistoryService(
            monthlySalesHistoryUpsertService = monthlySalesHistoryUpsertService,
            chunkedUpsertHelper = helper,
            auditService = auditService,
            chunkSize = 1000,
            maxRows = 50
        )
    }

    private fun item(
        sapAccountCode: String? = "1032619",
        salesYearMonth: String? = "202604"
    ): MonthlySalesHistoryRequestItem = MonthlySalesHistoryRequestItem(
        sapAccountCode = sapAccountCode,
        salesYearMonth = salesYearMonth
    )

    @Nested
    @DisplayName("upsert - 어댑터 책임")
    inner class AdapterResponsibilities {

        @Test
        @DisplayName("happy: 단일 청크, 도메인 결과 → SalesHistoryDetail + chunk SUCCESS + audit chunks=1")
        fun happy_singleChunkSuccess() {
            whenever(monthlySalesHistoryUpsertService.upsert(any())).thenReturn(
                MonthlySalesHistoryUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())
            )

            val detail = service.upsert(listOf(item()))

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.chunks.single().status).isEqualTo(ChunkResult.STATUS_SUCCESS)

            val auditCaptor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(auditCaptor.capture())
            assertThat(auditCaptor.firstValue.eventType).isEqualTo(SapInboundAuditEventType.REQUEST_ACCEPTED)
            assertThat(auditCaptor.firstValue.reason).isEqualTo("success=1 failure=0 chunks=1")
        }

        @Test
        @DisplayName("부분 실패: 도메인 failures → SAP FailureItem 매핑, chunk FAILED")
        fun partialFailure_chunkFailed() {
            whenever(monthlySalesHistoryUpsertService.upsert(any())).thenReturn(
                MonthlySalesHistoryUpsertResult(
                    successCount = 0,
                    failureCount = 1,
                    failures = listOf(MonthlySalesHistoryUpsertFailedRow("10326192026/04", "SalesYearMonth 형식 오류: 2026/04"))
                )
            )

            val detail = service.upsert(listOf(item(salesYearMonth = "2026/04")))

            assertThat(detail.successCount).isEqualTo(0)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.chunks.single().status).isEqualTo(ChunkResult.STATUS_FAILED)
        }

        @Test
        @DisplayName("청크 commit 실패: throw → chunk FAILED")
        fun chunkCommitFailure() {
            whenever(monthlySalesHistoryUpsertService.upsert(any()))
                .thenThrow(RuntimeException("DB connection lost"))

            val detail = service.upsert(listOf(item()))

            assertThat(detail.chunks.single().status).isEqualTo(ChunkResult.STATUS_FAILED)
            assertThat(detail.failureCount).isEqualTo(1)
        }

        @Test
        @DisplayName("DTO 매핑: MonthlySalesHistoryRequestItem → MonthlySalesHistoryUpsertCommand")
        fun dtoMapping_itemToCommand() {
            whenever(monthlySalesHistoryUpsertService.upsert(any())).thenReturn(
                MonthlySalesHistoryUpsertResult(successCount = 1, failureCount = 0, failures = emptyList())
            )
            val items = listOf(
                MonthlySalesHistoryRequestItem(
                    sapAccountCode = "1032619",
                    salesYearMonth = "202604",
                    abcClosingAmount1 = "5000000",
                    totalLedgerAmount = "1000000",
                    shipClosingAmount = "4800000"
                )
            )

            service.upsert(items)

            val captor = argumentCaptor<List<MonthlySalesHistoryUpsertCommand>>()
            verify(monthlySalesHistoryUpsertService).upsert(captor.capture())
            val command = captor.firstValue.single()
            assertThat(command.sapAccountCode).isEqualTo("1032619")
            assertThat(command.salesYearMonth).isEqualTo("202604")
            assertThat(command.abcClosingAmount1).isEqualTo("5000000")
            assertThat(command.totalLedgerAmount).isEqualTo("1000000")
            assertThat(command.shipClosingAmount).isEqualTo("4800000")
        }
    }

    @Nested
    @DisplayName("upsert - 어댑터 정책")
    inner class AdapterPolicies {

        @Test
        @DisplayName("행 수 한도 초과 - SapPayloadTooLargeException, 도메인 호출 없음")
        fun upsert_payloadTooLarge() {
            val items = (1..51).map { idx -> item(sapAccountCode = "ACCT-$idx") }

            assertThatThrownBy { service.upsert(items) }
                .isInstanceOf(SapPayloadTooLargeException::class.java)
            verify(monthlySalesHistoryUpsertService, never()).upsert(any())
        }
    }
}
