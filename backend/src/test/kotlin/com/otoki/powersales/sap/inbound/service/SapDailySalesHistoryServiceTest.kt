package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sales.entity.DailySalesHistory
import com.otoki.powersales.sap.inbound.dto.sales.ChunkResult
import com.otoki.powersales.sap.inbound.dto.sales.DailySalesHistoryRequestItem
import com.otoki.powersales.sap.inbound.dto.sales.FailureItem
import com.otoki.powersales.sap.inbound.exception.SapPayloadTooLargeException
import com.otoki.powersales.sales.repository.DailySalesHistoryRepository
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
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("SapDailySalesHistoryService 테스트")
class SapDailySalesHistoryServiceTest {

    @Mock
    private lateinit var dailySalesHistoryRepository: DailySalesHistoryRepository

    @Mock
    private lateinit var auditService: SapInboundAuditService

    private lateinit var service: SapDailySalesHistoryService

    @BeforeEach
    fun setUp() {
        // 단위 테스트에서는 Spring 프록시 없이 ChunkedUpsertHelper 를 직접 사용 (@Transactional 무효, action 직접 invoke)
        val helper = ChunkedUpsertHelper()
        service = SapDailySalesHistoryService(
            dailySalesHistoryRepository = dailySalesHistoryRepository,
            chunkedUpsertHelper = helper,
            auditService = auditService,
            chunkSize = 3,
            maxRows = 100
        )
    }

    private fun item(
        sapAccountCode: String? = "1032619",
        salesDate: String? = "20260427",
        erpSalesAmount1: String? = null,
        ledgerAmount: String? = null
    ): DailySalesHistoryRequestItem = DailySalesHistoryRequestItem(
        sapAccountCode = sapAccountCode,
        salesDate = salesDate,
        erpSalesAmount1 = erpSalesAmount1,
        ledgerAmount = ledgerAmount
    )

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 1건 - INSERT, success_count=1, chunk success")
        fun upsert_insertNew() {
            whenever(dailySalesHistoryRepository.findByExternalKeyIn(listOf("103261920260427")))
                .thenReturn(emptyList())

            val detail = service.upsert(listOf(item(erpSalesAmount1 = "1500000", ledgerAmount = "1500000")))

            val captor = argumentCaptor<List<DailySalesHistory>>()
            verify(dailySalesHistoryRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved.externalKey).isEqualTo("103261920260427")
            assertThat(saved.sapAccountCode).isEqualTo("1032619")
            assertThat(saved.salesDate).isEqualTo("20260427")
            assertThat(saved.erpSalesAmount1).isEqualTo(1500000.0)
            assertThat(saved.ledgerAmount).isEqualTo(1500000.0)
            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.chunks.single().status).isEqualTo(ChunkResult.STATUS_SUCCESS)
            verify(auditService).record(any<SapInboundAudit>())
        }

        @Test
        @DisplayName("기존 - UPDATE, mutable 금액만 갱신")
        fun upsert_updateExisting() {
            val existing = DailySalesHistory(
                sapAccountCode = "1032619",
                salesDate = "20260427",
                externalKey = "103261920260427"
            )
            existing.ledgerAmount = 1.0
            whenever(dailySalesHistoryRepository.findByExternalKeyIn(listOf("103261920260427")))
                .thenReturn(listOf(existing))

            service.upsert(listOf(item(ledgerAmount = "999")))

            val captor = argumentCaptor<List<DailySalesHistory>>()
            verify(dailySalesHistoryRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single()).isSameAs(existing)
            assertThat(existing.ledgerAmount).isEqualTo(999.0)
        }

        @Test
        @DisplayName("청크 분할 - 5건, chunkSize=3 → 2 chunks (3+2)")
        fun upsert_chunkSplit() {
            val items = (1..5).map { item(salesDate = "2026042$it") }
            whenever(dailySalesHistoryRepository.findByExternalKeyIn(any())).thenReturn(emptyList())

            val detail = service.upsert(items)

            assertThat(detail.successCount).isEqualTo(5)
            assertThat(detail.chunks).hasSize(2)
            assertThat(detail.chunks[0].count).isEqualTo(3)
            assertThat(detail.chunks[1].count).isEqualTo(2)
            assertThat(detail.chunks).allMatch { it.status == ChunkResult.STATUS_SUCCESS }
        }

        @Test
        @DisplayName("빈 금액 → 0")
        fun upsert_emptyAmountToZero() {
            whenever(dailySalesHistoryRepository.findByExternalKeyIn(any())).thenReturn(emptyList())

            service.upsert(listOf(item(erpSalesAmount1 = null, ledgerAmount = "")))

            val captor = argumentCaptor<List<DailySalesHistory>>()
            verify(dailySalesHistoryRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved.erpSalesAmount1).isEqualTo(0.0)
            assertThat(saved.ledgerAmount).isEqualTo(0.0)
        }
    }

    @Nested
    @DisplayName("upsert - Error Path")
    inner class UpsertError {

        @Test
        @DisplayName("행 수 한도 초과 - SapPayloadTooLargeException")
        fun upsert_payloadTooLarge() {
            val items = (1..101).map { item(salesDate = "20260" + (100 + it)) }

            assertThatThrownBy { service.upsert(items) }
                .isInstanceOf(SapPayloadTooLargeException::class.java)
            verify(dailySalesHistoryRepository, never()).saveAll(any<List<DailySalesHistory>>())
        }

        @Test
        @DisplayName("SalesDate 형식 오류 - 청크 partial, 해당 행 failed")
        fun upsert_invalidSalesDate() {
            whenever(dailySalesHistoryRepository.findByExternalKeyIn(any())).thenReturn(emptyList())

            val detail = service.upsert(listOf(item(salesDate = "2026-04-27"), item(salesDate = "20260428")))

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.chunks.single().status).isEqualTo(ChunkResult.STATUS_PARTIAL)
            assertThat(detail.failures.single().reason).contains("SalesDate 형식 오류")
        }

        @Test
        @DisplayName("청크 commit 실패 - chunks failed, 다른 청크는 success")
        fun upsert_chunkCommitFailure() {
            val items = (1..5).map { item(salesDate = "2026042$it") }
            // 첫 번째 청크 (3건) saveAll 호출 시 예외, 두 번째 청크는 정상
            whenever(dailySalesHistoryRepository.findByExternalKeyIn(any())).thenReturn(emptyList())
            var callCount = 0
            doAnswer {
                callCount++
                if (callCount == 1) throw RuntimeException("DB connection lost")
                @Suppress("UNCHECKED_CAST")
                it.arguments[0] as List<DailySalesHistory>
            }.whenever(dailySalesHistoryRepository).saveAll(any<List<DailySalesHistory>>())

            val detail = service.upsert(items)

            assertThat(detail.chunks).hasSize(2)
            assertThat(detail.chunks[0].status).isEqualTo(ChunkResult.STATUS_FAILED)
            assertThat(detail.chunks[1].status).isEqualTo(ChunkResult.STATUS_SUCCESS)
            assertThat(detail.successCount).isEqualTo(2) // 두 번째 청크만
            assertThat(detail.failureCount).isEqualTo(3) // 첫 번째 청크 전체
        }

        @Test
        @DisplayName("SAPAccountCode 누락 - 행 failures")
        fun upsert_missingSapAccountCode() {
            val detail = service.upsert(listOf(item(sapAccountCode = null)))

            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().reason).contains("SAPAccountCode 필수")
        }
    }
}
