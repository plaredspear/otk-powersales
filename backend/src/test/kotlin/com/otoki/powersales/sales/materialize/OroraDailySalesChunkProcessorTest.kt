package com.otoki.powersales.sales.materialize

import com.otoki.orora.entity.OroraDailySalesHistory
import com.otoki.orora.repository.OroraDailySalesHistoryRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.sales.entity.DailySalesHistory
import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.sales.enums.SalesMonth
import com.otoki.powersales.sales.enums.SalesYear
import com.otoki.powersales.sales.repository.DailySalesHistoryRepository
import com.otoki.powersales.sales.repository.MonthlySalesHistoryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * [OroraDailySalesChunkProcessor] 일별 적재 + 월별 합계 갱신 검증 (Spec #855).
 */
@DisplayName("OroraDailySalesChunkProcessor")
class OroraDailySalesChunkProcessorTest {

    private val ororaRepo: OroraDailySalesHistoryRepository = mockk()
    private val dailyRepo: DailySalesHistoryRepository = mockk()
    private val monthlyRepo: MonthlySalesHistoryRepository = mockk()
    private val accountRepo: AccountRepository = mockk()
    private val processor = OroraDailySalesChunkProcessor(ororaRepo, dailyRepo, monthlyRepo, accountRepo)

    private fun ororaRow(sap: String, salesDate: String, erpSales: String, erpDist: String) =
        OroraDailySalesHistory(
            sapAccountCode = sap,
            salesDate = salesDate,
            erpSalesAmount = BigDecimal(erpSales),
            erpDistributionAmount = BigDecimal(erpDist),
        )

    @Test
    @DisplayName("일별 upsert: SalesDate 원본 YYYYMMDD 보관 + 단일 금액 2컬럼만 + external_key")
    fun upsertDailyPreservesOriginalDate() {
        every {
            ororaRepo.findBySalesDateStartingWithAndSapAccountCodeBetween("202605", "0001000000", "0001001999")
        } returns listOf(ororaRow("0001000077", "20260515", "1000", "200"))
        every { accountRepo.findByExternalKeyIn(listOf("1000077")) } returns
            listOf(Account(id = 5, externalKey = "1000077", sfid = "001ACC"))
        every { dailyRepo.findByExternalKeyIn(listOf("100007720260515")) } returns emptyList()
        val savedDaily = slot<List<DailySalesHistory>>()
        every { dailyRepo.saveAll(capture(savedDaily)) } answers { savedDaily.captured }
        // 월별 합계 갱신 경로
        every {
            monthlyRepo.findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(any(), any(), any())
        } returns emptyList()
        every { dailyRepo.findBySapAccountCodeAndSalesDateStartingWith("1000077", "202605") } returns
            listOf(
                DailySalesHistory(sapAccountCode = "1000077", salesDate = "20260515", externalKey = "100007720260515")
                    .apply { erpSalesAmount = 1000.0; erpDistributionAmount = 200.0 }
            )
        val savedMonthly = slot<List<MonthlySalesHistory>>()
        every { monthlyRepo.saveAll(capture(savedMonthly)) } answers { savedMonthly.captured }

        val result = processor.process("202605", SalesYear.Y2026, SalesMonth.M05, "0001000000", "0001001999")

        assertThat(result.dailyUpserted).isEqualTo(1)
        val daily = savedDaily.captured.first()
        assertThat(daily.salesDate).isEqualTo("20260515") // 원본 보관 (보정 없음)
        assertThat(daily.externalKey).isEqualTo("100007720260515")
        assertThat(daily.erpSalesAmount).isEqualTo(1000.0)
        assertThat(daily.erpDistributionAmount).isEqualTo(200.0)
        assertThat(daily.account?.id).isEqualTo(5L)
    }

    @Test
    @DisplayName("월별 합계 갱신: total_ledger_amount = daily (erp_sales + erp_distribution) 합")
    fun updatesMonthlyTotalLedger() {
        every {
            ororaRepo.findBySalesDateStartingWithAndSapAccountCodeBetween(any(), any(), any())
        } returns listOf(ororaRow("0001000077", "20260515", "1000", "200"))
        every { accountRepo.findByExternalKeyIn(any()) } returns emptyList()
        every { dailyRepo.findByExternalKeyIn(any()) } returns emptyList()
        every { dailyRepo.saveAll(any<List<DailySalesHistory>>()) } answers { firstArg() }
        every {
            monthlyRepo.findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(any(), any(), any())
        } returns emptyList()
        // 한 달치 daily 2건 (515: 1000+200, 516: 500+100) → ledger 합 1800
        every { dailyRepo.findBySapAccountCodeAndSalesDateStartingWith("1000077", "202605") } returns
            listOf(
                DailySalesHistory(sapAccountCode = "1000077", salesDate = "20260515", externalKey = "k1")
                    .apply { erpSalesAmount = 1000.0; erpDistributionAmount = 200.0 },
                DailySalesHistory(sapAccountCode = "1000077", salesDate = "20260516", externalKey = "k2")
                    .apply { erpSalesAmount = 500.0; erpDistributionAmount = 100.0 },
            )
        val savedMonthly = slot<List<MonthlySalesHistory>>()
        every { monthlyRepo.saveAll(capture(savedMonthly)) } answers { savedMonthly.captured }

        val result = processor.process("202605", SalesYear.Y2026, SalesMonth.M05, "0001000000", "0001001999")

        assertThat(result.monthlyUpdated).isEqualTo(1)
        assertThat(savedMonthly.captured.first().totalLedgerAmount).isEqualByComparingTo(BigDecimal("1800"))
    }
}
