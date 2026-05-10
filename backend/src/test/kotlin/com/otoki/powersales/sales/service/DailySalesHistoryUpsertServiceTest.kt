package com.otoki.powersales.sales.service

import com.otoki.powersales.sales.entity.DailySalesHistory
import com.otoki.powersales.sales.repository.DailySalesHistoryRepository
import com.otoki.powersales.sales.service.dto.DailySalesHistoryUpsertCommand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("DailySalesHistoryUpsertService 테스트")
class DailySalesHistoryUpsertServiceTest {

    @Mock
    private lateinit var dailySalesHistoryRepository: DailySalesHistoryRepository

    @InjectMocks
    private lateinit var service: DailySalesHistoryUpsertService

    private fun command(
        sapAccountCode: String? = "1032619",
        salesDate: String? = "20260427",
        erpSalesAmount1: String? = null,
        ledgerAmount: String? = null
    ): DailySalesHistoryUpsertCommand = DailySalesHistoryUpsertCommand(
        sapAccountCode = sapAccountCode,
        salesDate = salesDate,
        erpSalesAmount1 = erpSalesAmount1,
        erpSalesAmount2 = null,
        erpSalesAmount3 = null,
        erpDistributionAmount1 = null,
        erpDistributionAmount2 = null,
        erpDistributionAmount3 = null,
        ledgerAmount = ledgerAmount
    )

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 1건 - INSERT, success_count=1")
        fun upsert_insertNew() {
            whenever(dailySalesHistoryRepository.findByExternalKeyIn(listOf("103261920260427")))
                .thenReturn(emptyList())

            val result = service.upsert(listOf(command(erpSalesAmount1 = "1500000", ledgerAmount = "1500000")))

            val captor = argumentCaptor<List<DailySalesHistory>>()
            verify(dailySalesHistoryRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved.externalKey).isEqualTo("103261920260427")
            assertThat(saved.sapAccountCode).isEqualTo("1032619")
            assertThat(saved.salesDate).isEqualTo("20260427")
            assertThat(saved.erpSalesAmount1).isEqualTo(1500000.0)
            assertThat(saved.ledgerAmount).isEqualTo(1500000.0)
            assertThat(result.successCount).isEqualTo(1)
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

            service.upsert(listOf(command(ledgerAmount = "999")))

            val captor = argumentCaptor<List<DailySalesHistory>>()
            verify(dailySalesHistoryRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single()).isSameAs(existing)
            assertThat(existing.ledgerAmount).isEqualTo(999.0)
        }

        @Test
        @DisplayName("빈 금액 → 0")
        fun upsert_emptyAmountToZero() {
            whenever(dailySalesHistoryRepository.findByExternalKeyIn(any())).thenReturn(emptyList())

            service.upsert(listOf(command(erpSalesAmount1 = null, ledgerAmount = "")))

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
        @DisplayName("SAPAccountCode 누락 - failures, identifier null, 적재 스킵")
        fun upsert_missingSapAccountCode() {
            val result = service.upsert(listOf(command(sapAccountCode = null)))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isNull()
            assertThat(result.failures.single().reason).contains("SAPAccountCode 필수")
            verify(dailySalesHistoryRepository, never()).saveAll(any<List<DailySalesHistory>>())
        }

        @Test
        @DisplayName("SalesDate 형식 오류 - failures (identifier=ac+sd)")
        fun upsert_invalidSalesDate() {
            whenever(dailySalesHistoryRepository.findByExternalKeyIn(any())).thenReturn(emptyList())

            val result = service.upsert(listOf(command(salesDate = "2026-04-27")))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("10326192026-04-27")
            assertThat(result.failures.single().reason).contains("SalesDate 형식 오류")
        }

        @Test
        @DisplayName("일부 행 실패 - 성공 행은 적재, 실패 행은 failures 누적")
        fun upsert_partialFailure() {
            whenever(dailySalesHistoryRepository.findByExternalKeyIn(any())).thenReturn(emptyList())

            val result = service.upsert(
                listOf(
                    command(salesDate = "2026-04-27"),
                    command(salesDate = "20260428", erpSalesAmount1 = "100")
                )
            )

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(1)
        }
    }
}
