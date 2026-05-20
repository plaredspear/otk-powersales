package com.otoki.powersales.sales.service

import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.sales.enums.SalesMonth
import com.otoki.powersales.sales.enums.SalesYear
import com.otoki.powersales.sales.repository.MonthlySalesHistoryRepository
import com.otoki.powersales.sales.service.dto.MonthlySalesHistoryUpsertCommand
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("MonthlySalesHistoryUpsertService 테스트")
class MonthlySalesHistoryUpsertServiceTest {

    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository = mockk()

    private val service = MonthlySalesHistoryUpsertService(
        monthlySalesHistoryRepository,
    )

    private fun command(
        sapAccountCode: String? = "1032619",
        salesYearMonth: String? = "202604",
        abcClosingAmount1: String? = null,
        shipClosingAmount: String? = null,
        rlsales: String? = null,
        totalLedgerAmount: String? = null
    ): MonthlySalesHistoryUpsertCommand = MonthlySalesHistoryUpsertCommand(
        sapAccountCode = sapAccountCode,
        salesYearMonth = salesYearMonth,
        abcClosingAmount1 = abcClosingAmount1,
        abcClosingAmount2 = null,
        abcClosingAmount3 = null,
        totalLedgerAmount = totalLedgerAmount,
        shipClosingAmount = shipClosingAmount,
        rlsales = rlsales
    )

    private fun stubSaveAllCapture(): io.mockk.CapturingSlot<List<MonthlySalesHistory>> {
        val slot = slot<List<MonthlySalesHistory>>()
        every { monthlySalesHistoryRepository.saveAll(capture(slot)) } answers { firstArg<List<MonthlySalesHistory>>() }
        return slot
    }

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 - INSERT, salesYear=2026, salesMonth=04")
        fun upsert_insertNew() {
            every { monthlySalesHistoryRepository.findByExternalkeyCIn(listOf("1032619202604")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            val result = service.upsert(
                listOf(command(abcClosingAmount1 = "5000000", shipClosingAmount = "4800000", rlsales = "0"))
            )

            val saved = savedSlot.captured.single()
            assertThat(saved.externalkeyC).isEqualTo("1032619202604")
            assertThat(saved.salesYear).isEqualTo(SalesYear.Y2026)
            assertThat(saved.salesMonth).isEqualTo(SalesMonth.M04)
            assertThat(saved.abcClosingAmount1).isEqualTo(5000000.0)
            assertThat(saved.shipClosingAmount).isEqualTo(4800000.0)
            assertThat(saved.rlsalesC).isEqualTo(0.0)
            assertThat(result.successCount).isEqualTo(1)
        }

        @Test
        @DisplayName("기존 - UPDATE")
        fun upsert_updateExisting() {
            val existing = MonthlySalesHistory(externalkeyC = "1032619202604")
            existing.shipClosingAmount = 1.0
            every { monthlySalesHistoryRepository.findByExternalkeyCIn(listOf("1032619202604")) } returns listOf(existing)
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(shipClosingAmount = "9999")))

            assertThat(savedSlot.captured.single()).isSameAs(existing)
            assertThat(existing.shipClosingAmount).isEqualTo(9999.0)
        }

        @Test
        @DisplayName("Spec #575 - TotalLedgerAmount 정상 매핑")
        fun upsert_totalLedgerAmountMapped() {
            every { monthlySalesHistoryRepository.findByExternalkeyCIn(listOf("1032619202604")) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(totalLedgerAmount = "1000000")))

            assertThat(savedSlot.captured.single().totalLedgerAmount).isEqualByComparingTo(BigDecimal("1000000"))
        }

        @Test
        @DisplayName("Spec #575 - TotalLedgerAmount blank → 0")
        fun upsert_totalLedgerAmountBlank() {
            every { monthlySalesHistoryRepository.findByExternalkeyCIn(any()) } returns emptyList()
            val savedSlot = stubSaveAllCapture()

            service.upsert(listOf(command(totalLedgerAmount = "")))

            assertThat(savedSlot.captured.single().totalLedgerAmount).isEqualByComparingTo(BigDecimal.ZERO)
        }
    }

    @Nested
    @DisplayName("upsert - Error Path")
    inner class UpsertError {

        @Test
        @DisplayName("SalesYearMonth 형식 오류")
        fun upsert_invalidYearMonth() {
            every { monthlySalesHistoryRepository.findByExternalkeyCIn(any()) } returns emptyList()
            every { monthlySalesHistoryRepository.saveAll(any<List<MonthlySalesHistory>>()) } answers { firstArg<List<MonthlySalesHistory>>() }

            val result = service.upsert(listOf(command(salesYearMonth = "2026/04")))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("SalesYearMonth 형식 오류")
        }

        @Test
        @DisplayName("ABCClosingAmount1 변환 실패")
        fun upsert_invalidAmount() {
            every { monthlySalesHistoryRepository.findByExternalkeyCIn(any()) } returns emptyList()
            every { monthlySalesHistoryRepository.saveAll(any<List<MonthlySalesHistory>>()) } answers { firstArg<List<MonthlySalesHistory>>() }

            val result = service.upsert(listOf(command(abcClosingAmount1 = "abc")))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("금액 변환 실패")
        }

        @Test
        @DisplayName("월 범위 오류 (13)")
        fun upsert_invalidMonth() {
            every { monthlySalesHistoryRepository.findByExternalkeyCIn(any()) } returns emptyList()
            every { monthlySalesHistoryRepository.saveAll(any<List<MonthlySalesHistory>>()) } answers { firstArg<List<MonthlySalesHistory>>() }

            val result = service.upsert(listOf(command(salesYearMonth = "202613")))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("월 범위 오류")
        }

        @Test
        @DisplayName("Spec #575 - TotalLedgerAmount 비숫자 → 행 단위 부분 실패")
        fun upsert_totalLedgerAmountInvalid() {
            every { monthlySalesHistoryRepository.findByExternalkeyCIn(any()) } returns emptyList()
            every { monthlySalesHistoryRepository.saveAll(any<List<MonthlySalesHistory>>()) } answers { firstArg<List<MonthlySalesHistory>>() }

            val result = service.upsert(listOf(command(totalLedgerAmount = "abc")))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("금액 변환 실패")
        }
    }
}
