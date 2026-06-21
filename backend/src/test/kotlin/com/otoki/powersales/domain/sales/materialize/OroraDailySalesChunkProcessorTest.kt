package com.otoki.powersales.domain.sales.materialize

import com.otoki.orora.entity.OroraDailySalesHistory
import com.otoki.orora.repository.OroraDailySalesHistoryRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.sales.entity.DailySalesHistory
import com.otoki.powersales.domain.sales.entity.MonthlySalesHistory
import com.otoki.powersales.domain.sales.enums.SalesMonth
import com.otoki.powersales.domain.sales.enums.SalesYear
import com.otoki.powersales.domain.sales.repository.DailySalesHistoryRepository
import com.otoki.powersales.domain.sales.repository.MonthlySalesHistoryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * [OroraDailySalesChunkProcessor] 일별 적재 + 월별 합계 갱신 검증 (Spec #855 / 레거시 raw 동작 정합).
 *
 * 레거시 DailyErpSalesInfoTriggerHandler 동등:
 * - SalesDate 보정 (대상월≠today연월 → 그 달 말일), external key 는 원본 YYYYMMDD.
 * - 거래처 미매칭 row 차단.
 * - 월별 = abcClosingSum(ΣERPSales) + shipClosingSum(ΣERPDist) + totalLedger(ΣLedger=0).
 * - insert 경로=합산, update 경로=마지막 1건 값 덮어쓰기.
 */
@DisplayName("OroraDailySalesChunkProcessor — 레거시 raw 동작 정합")
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

    private fun account(id: Long, key: String) = Account(id = id, externalKey = key, sfid = "001$key")

    @Test
    @DisplayName("일별 upsert: SalesDate 는 과거 대상월 말일로 보정 + external_key 는 원본 YYYYMMDD")
    fun upsertDailyCorrectsSalesDateButKeepsOriginalKey() {
        // 대상월 202601 (today=2026-06 과 다름) → 저장 SalesDate=2026-01-31, key=원본 20260115
        every {
            ororaRepo.findBySalesDateStartingWithAndSapAccountCodeBetween("202601", "0001000000", "0001001999")
        } returns listOf(ororaRow("0001000077", "20260115", "1000", "200"))
        every { accountRepo.findByExternalKeyIn(listOf("1000077")) } returns listOf(account(5, "1000077"))
        every { dailyRepo.findByExternalKeyIn(listOf("100007720260115")) } returns emptyList()
        val savedDaily = slot<List<DailySalesHistory>>()
        every { dailyRepo.saveAll(capture(savedDaily)) } answers { savedDaily.captured }
        every {
            monthlyRepo.findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(any(), any(), any())
        } returns emptyList()
        every { monthlyRepo.saveAll(any<List<MonthlySalesHistory>>()) } answers { firstArg<List<MonthlySalesHistory>>() }

        val result = processor.process("202601", SalesYear.Y2026, SalesMonth.M01, "0001000000", "0001001999")

        assertThat(result.dailyUpserted).isEqualTo(1)
        val daily = savedDaily.captured.first()
        assertThat(daily.salesDate).isEqualTo("20260131") // 과거 대상월 → 말일 보정
        assertThat(daily.externalKey).isEqualTo("100007720260115") // 원본 일자 기준
        assertThat(daily.erpSalesAmount).isEqualTo(1000.0)
        assertThat(daily.erpDistributionAmount).isEqualTo(200.0)
        assertThat(daily.account?.id).isEqualTo(5L)
    }

    @Test
    @DisplayName("거래처 미매칭 row 는 일별·월별 모두 생성 안 함 (레거시 addError 동등)")
    fun unmatchedAccountRowIsSkipped() {
        every {
            ororaRepo.findBySalesDateStartingWithAndSapAccountCodeBetween(any(), any(), any())
        } returns listOf(ororaRow("0001000077", "20260115", "1000", "200"))
        every { accountRepo.findByExternalKeyIn(any()) } returns emptyList() // 매칭 0건
        every { dailyRepo.findByExternalKeyIn(any()) } returns emptyList()
        val savedDaily = slot<List<DailySalesHistory>>()
        every { dailyRepo.saveAll(capture(savedDaily)) } answers { savedDaily.captured }
        every {
            monthlyRepo.findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(any(), any(), any())
        } returns emptyList()
        every { monthlyRepo.saveAll(any<List<MonthlySalesHistory>>()) } answers { firstArg<List<MonthlySalesHistory>>() }

        val result = processor.process("202601", SalesYear.Y2026, SalesMonth.M01, "0001000000", "0001001999")

        assertThat(result.dailyUpserted).isEqualTo(0)
        assertThat(result.monthlyUpdated).isEqualTo(0)
        assertThat(savedDaily.captured).isEmpty()
    }

    @Test
    @DisplayName("월별 insert 경로: 신규 일별 row 들의 합산 (abcSum=ΣERPSales, shipSum=ΣERPDist, ledger=0)")
    fun insertPathSumsNewRows() {
        // 신규 일별 2건 (1000+200, 500+100) → abcSum=1500, shipSum=300, ledger=0
        every {
            ororaRepo.findBySalesDateStartingWithAndSapAccountCodeBetween(any(), any(), any())
        } returns listOf(
            ororaRow("0001000077", "20260115", "1000", "200"),
            ororaRow("0001000077", "20260116", "500", "100"),
        )
        every { accountRepo.findByExternalKeyIn(any()) } returns listOf(account(5, "1000077"))
        every { dailyRepo.findByExternalKeyIn(any()) } returns emptyList() // 둘 다 신규 = insert 경로
        every { dailyRepo.saveAll(any<List<DailySalesHistory>>()) } answers { firstArg<List<DailySalesHistory>>() }
        every {
            monthlyRepo.findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(any(), any(), any())
        } returns emptyList()
        val savedMonthly = slot<List<MonthlySalesHistory>>()
        every { monthlyRepo.saveAll(capture(savedMonthly)) } answers { savedMonthly.captured }

        val result = processor.process("202601", SalesYear.Y2026, SalesMonth.M01, "0001000000", "0001001999")

        assertThat(result.monthlyUpdated).isEqualTo(1)
        val m = savedMonthly.captured.first()
        assertThat(m.abcClosingSumAmount).isEqualTo(1500.0)
        assertThat(m.shipClosingSumAmount).isEqualTo(300.0)
        assertThat(m.totalLedgerAmount).isEqualByComparingTo(BigDecimal.ZERO) // 일별 ledger 미적재 → 0
    }

    @Test
    @DisplayName("월별 update 경로: 기존 일별 row 들의 마지막 1건 값으로 덮어쓰기 (레거시 = 단일 대입)")
    fun updatePathOverwritesWithLastRow() {
        val existingByKey = listOf(
            DailySalesHistory(sapAccountCode = "1000077", salesDate = "20260131", externalKey = "100007720260115"),
            DailySalesHistory(sapAccountCode = "1000077", salesDate = "20260131", externalKey = "100007720260116"),
        )
        every {
            ororaRepo.findBySalesDateStartingWithAndSapAccountCodeBetween(any(), any(), any())
        } returns listOf(
            ororaRow("0001000077", "20260115", "1000", "200"),
            ororaRow("0001000077", "20260116", "500", "100"), // 마지막 row → 이 값만 남아야 함
        )
        every { accountRepo.findByExternalKeyIn(any()) } returns listOf(account(5, "1000077"))
        every { dailyRepo.findByExternalKeyIn(any()) } returns existingByKey // 둘 다 기존 = update 경로
        every { dailyRepo.saveAll(any<List<DailySalesHistory>>()) } answers { firstArg<List<DailySalesHistory>>() }
        every {
            monthlyRepo.findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(any(), any(), any())
        } returns emptyList()
        val savedMonthly = slot<List<MonthlySalesHistory>>()
        every { monthlyRepo.saveAll(capture(savedMonthly)) } answers { savedMonthly.captured }

        val result = processor.process("202601", SalesYear.Y2026, SalesMonth.M01, "0001000000", "0001001999")

        assertThat(result.monthlyUpdated).isEqualTo(1)
        val m = savedMonthly.captured.first()
        // 합산(1500/300)이 아니라 마지막 row(500/100) 값으로 덮어써야 한다 (레거시 buggy update 경로 동등).
        assertThat(m.abcClosingSumAmount).isEqualTo(500.0)
        assertThat(m.shipClosingSumAmount).isEqualTo(100.0)
        assertThat(m.totalLedgerAmount).isEqualByComparingTo(BigDecimal.ZERO)
    }
}
