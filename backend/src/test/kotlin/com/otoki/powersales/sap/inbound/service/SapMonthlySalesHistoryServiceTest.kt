package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.entity.MonthlySalesHistory
import com.otoki.powersales.sap.inbound.dto.sales.ChunkResult
import com.otoki.powersales.sap.inbound.dto.sales.MonthlySalesHistoryRequestItem
import com.otoki.powersales.sap.inbound.exception.SapPayloadTooLargeException
import com.otoki.powersales.sap.repository.MonthlySalesHistoryRepository
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
@DisplayName("SapMonthlySalesHistoryService 테스트")
class SapMonthlySalesHistoryServiceTest {

    @Mock
    private lateinit var monthlySalesHistoryRepository: MonthlySalesHistoryRepository

    @Mock
    private lateinit var auditService: SapInboundAuditService

    private lateinit var service: SapMonthlySalesHistoryService

    @BeforeEach
    fun setUp() {
        val helper = ChunkedUpsertHelper()
        service = SapMonthlySalesHistoryService(
            monthlySalesHistoryRepository = monthlySalesHistoryRepository,
            chunkedUpsertHelper = helper,
            auditService = auditService,
            chunkSize = 1000,
            maxRows = 50
        )
    }

    private fun item(
        sapAccountCode: String? = "1032619",
        salesYearMonth: String? = "202604",
        abcClosingAmount1: String? = null,
        shipClosingAmount: String? = null,
        rlsales: String? = null
    ): MonthlySalesHistoryRequestItem = MonthlySalesHistoryRequestItem(
        sapAccountCode = sapAccountCode,
        salesYearMonth = salesYearMonth,
        abcClosingAmount1 = abcClosingAmount1,
        shipClosingAmount = shipClosingAmount,
        rlsales = rlsales
    )

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 - INSERT, salesYear=2026, salesMonth=04")
        fun upsert_insertNew() {
            whenever(monthlySalesHistoryRepository.findByExternalkeyCIn(listOf("1032619202604")))
                .thenReturn(emptyList())

            val detail = service.upsert(
                listOf(item(abcClosingAmount1 = "5000000", shipClosingAmount = "4800000", rlsales = "0"))
            )

            val captor = argumentCaptor<List<MonthlySalesHistory>>()
            verify(monthlySalesHistoryRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved.externalkeyC).isEqualTo("1032619202604")
            assertThat(saved.salesYear).isEqualTo("2026")
            assertThat(saved.salesMonth).isEqualTo("04")
            assertThat(saved.abcClosingAmount1).isEqualTo(5000000.0)
            assertThat(saved.shipClosingAmount).isEqualTo(4800000.0)
            assertThat(saved.rlsalesC).isEqualTo(0.0)
            assertThat(detail.successCount).isEqualTo(1)
            verify(auditService).record(any<SapInboundAudit>())
        }

        @Test
        @DisplayName("기존 - UPDATE")
        fun upsert_updateExisting() {
            val existing = MonthlySalesHistory(externalkeyC = "1032619202604")
            existing.shipClosingAmount = 1.0
            whenever(monthlySalesHistoryRepository.findByExternalkeyCIn(listOf("1032619202604")))
                .thenReturn(listOf(existing))

            service.upsert(listOf(item(shipClosingAmount = "9999")))

            val captor = argumentCaptor<List<MonthlySalesHistory>>()
            verify(monthlySalesHistoryRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single()).isSameAs(existing)
            assertThat(existing.shipClosingAmount).isEqualTo(9999.0)
        }
    }

    @Nested
    @DisplayName("upsert - Error Path")
    inner class UpsertError {

        @Test
        @DisplayName("행 수 한도 초과")
        fun upsert_payloadTooLarge() {
            val items = (1..51).map { idx -> item(sapAccountCode = "ACCT-$idx") }

            assertThatThrownBy { service.upsert(items) }
                .isInstanceOf(SapPayloadTooLargeException::class.java)
            verify(monthlySalesHistoryRepository, never()).saveAll(any<List<MonthlySalesHistory>>())
        }

        @Test
        @DisplayName("SalesYearMonth 형식 오류")
        fun upsert_invalidYearMonth() {
            whenever(monthlySalesHistoryRepository.findByExternalkeyCIn(any())).thenReturn(emptyList())

            val detail = service.upsert(listOf(item(salesYearMonth = "2026/04")))

            assertThat(detail.successCount).isEqualTo(0)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.chunks.single().status).isEqualTo(ChunkResult.STATUS_FAILED)
        }

        @Test
        @DisplayName("ABCClosingAmount1 변환 실패")
        fun upsert_invalidAmount() {
            whenever(monthlySalesHistoryRepository.findByExternalkeyCIn(any())).thenReturn(emptyList())

            val detail = service.upsert(listOf(item(abcClosingAmount1 = "abc")))

            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().reason).contains("금액 변환 실패")
        }

        @Test
        @DisplayName("월 범위 오류 (13)")
        fun upsert_invalidMonth() {
            whenever(monthlySalesHistoryRepository.findByExternalkeyCIn(any())).thenReturn(emptyList())

            val detail = service.upsert(listOf(item(salesYearMonth = "202613")))

            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().reason).contains("월 범위 오류")
        }
    }
}
