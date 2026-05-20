package com.otoki.powersales.sales.service

import com.otoki.powersales.sales.entity.DailySalesHistory
import com.otoki.powersales.sales.repository.DailySalesHistoryRepository
import com.otoki.powersales.sales.service.dto.DailySalesHistoryUpsertCommand
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DailySalesHistoryUpsertService 테스트")
class DailySalesHistoryUpsertServiceTest {

    private val dailySalesHistoryRepository: DailySalesHistoryRepository = mockk()

    private val service = DailySalesHistoryUpsertService(
        dailySalesHistoryRepository,
    )

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

    private fun stubSaveAllCapture(): io.mockk.CapturingSlot<List<DailySalesHistory>> {
        val slot = slot<List<DailySalesHistory>>()
        every { dailySalesHistoryRepository.saveAll(capture(slot)) } answers { firstArg<List<DailySalesHistory>>() }
        return slot
    }

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 1건 - INSERT, success_count=1")
        fun upsert_insertNew() {
            every { dailySalesHistoryRepository.findByExternalKeyIn(listOf("103261920260427")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            val result = service.upsert(listOf(command(erpSalesAmount1 = "1500000", ledgerAmount = "1500000")))

            val saved = savedSlot.captured.single()
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
            every { dailySalesHistoryRepository.findByExternalKeyIn(listOf("103261920260427")) } returns listOf(existing)
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(ledgerAmount = "999")))

            assertThat(savedSlot.captured.single()).isSameAs(existing)
            assertThat(existing.ledgerAmount).isEqualTo(999.0)
        }

        @Test
        @DisplayName("빈 금액 → 0")
        fun upsert_emptyAmountToZero() {
            every { dailySalesHistoryRepository.findByExternalKeyIn(any()) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(erpSalesAmount1 = null, ledgerAmount = "")))

            val saved = savedSlot.captured.single()
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
            verify(exactly = 0) { dailySalesHistoryRepository.saveAll(any<List<DailySalesHistory>>()) }
        }

        @Test
        @DisplayName("SalesDate 형식 오류 - failures (identifier=ac+sd)")
        fun upsert_invalidSalesDate() {
            every { dailySalesHistoryRepository.findByExternalKeyIn(any()) } returns emptyList()
            every { dailySalesHistoryRepository.saveAll(any<List<DailySalesHistory>>()) } answers { firstArg<List<DailySalesHistory>>() }

            val result = service.upsert(listOf(command(salesDate = "2026-04-27")))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("10326192026-04-27")
            assertThat(result.failures.single().reason).contains("SalesDate 형식 오류")
        }

        @Test
        @DisplayName("일부 행 실패 - 성공 행은 적재, 실패 행은 failures 누적")
        fun upsert_partialFailure() {
            every { dailySalesHistoryRepository.findByExternalKeyIn(any()) } returns emptyList()
            every { dailySalesHistoryRepository.saveAll(any<List<DailySalesHistory>>()) } answers { firstArg<List<DailySalesHistory>>() }

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
