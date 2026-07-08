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
 * [OroraDailySalesChunkProcessor] 일별 적재 + 월별 합계 갱신 검증.
 *
 * 레거시 DailyErpSalesInfoTriggerHandler 동등:
 * - SalesDate 보정 (대상월≠today연월 → 그 달 말일), external key 는 원본 YYYYMMDD.
 * - 거래처 미매칭 row 차단.
 *
 * 레거시 deviation (사용자 결정 2026-07-09):
 * - 월별 합계는 해당 거래처+월 daily 전체 **재합산** (abcSum=ΣERPSales, shipSum=ΣERPDist, ledger=ΣLedger).
 *   레거시의 "처리분 기준 대입" (insert=합산/update=마지막 1건 값) 은 재실행 시 값이 달라지는 비멱등이라 교체.
 */
@DisplayName("OroraDailySalesChunkProcessor — 일별 적재 + 월별 재합산 (멱등)")
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

    private fun dailyRow(
        sap: String,
        externalKey: String,
        erpSales: Double?,
        erpDist: Double?,
        ledger: Double? = null,
    ) = DailySalesHistory(
        sapAccountCode = sap,
        salesDate = "20260131",
        externalKey = externalKey,
        erpSalesAmount = erpSales,
        erpDistributionAmount = erpDist,
        ledgerAmount = ledger,
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
        every { dailyRepo.findBySapAccountCodeInAndSalesDateStartingWith(any(), any()) } returns emptyList()
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

        val result = processor.process("202601", SalesYear.Y2026, SalesMonth.M01, "0001000000", "0001001999")

        assertThat(result.dailyUpserted).isEqualTo(0)
        assertThat(result.monthlyUpdated).isEqualTo(0)
        assertThat(savedDaily.captured).isEmpty()
    }

    @Test
    @DisplayName("월별 재합산: 적재 후 해당 거래처+월 daily 전체를 재조회하여 합산")
    fun monthlyAggregateResumsWholeMonth() {
        // 신규 일별 2건 적재 → 월 재조회 결과(적재분 반영) 합산: abcSum=1500, shipSum=300, ledger=0
        every {
            ororaRepo.findBySalesDateStartingWithAndSapAccountCodeBetween(any(), any(), any())
        } returns listOf(
            ororaRow("0001000077", "20260115", "1000", "200"),
            ororaRow("0001000077", "20260116", "500", "100"),
        )
        every { accountRepo.findByExternalKeyIn(any()) } returns listOf(account(5, "1000077"))
        every { dailyRepo.findByExternalKeyIn(any()) } returns emptyList()
        every { dailyRepo.saveAll(any<List<DailySalesHistory>>()) } answers { firstArg<List<DailySalesHistory>>() }
        every {
            dailyRepo.findBySapAccountCodeInAndSalesDateStartingWith(listOf("1000077"), "202601")
        } returns listOf(
            dailyRow("1000077", "100007720260115", 1000.0, 200.0),
            dailyRow("1000077", "100007720260116", 500.0, 100.0),
        )
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
        assertThat(m.totalLedgerAmount).isEqualByComparingTo(BigDecimal.ZERO) // ORORA 일별 ledger 미적재 → 0
    }

    @Test
    @DisplayName("멱등성: 동일 데이터 재수신(전건 기존 row) 시에도 월 합계가 재합산으로 유지")
    fun rerunKeepsSameMonthlyAggregate() {
        // 레거시 raw 동작이라면 update 경로 마지막 1건 값(500/100)으로 덮어썼을 케이스 —
        // 재합산 방식은 재실행해도 항상 월 전체 합(1500/300)으로 수렴해야 한다.
        val existingRows = listOf(
            dailyRow("1000077", "100007720260115", 1000.0, 200.0),
            dailyRow("1000077", "100007720260116", 500.0, 100.0),
        )
        every {
            ororaRepo.findBySalesDateStartingWithAndSapAccountCodeBetween(any(), any(), any())
        } returns listOf(
            ororaRow("0001000077", "20260115", "1000", "200"),
            ororaRow("0001000077", "20260116", "500", "100"),
        )
        every { accountRepo.findByExternalKeyIn(any()) } returns listOf(account(5, "1000077"))
        every { dailyRepo.findByExternalKeyIn(any()) } returns existingRows // 둘 다 기존 = 재실행
        every { dailyRepo.saveAll(any<List<DailySalesHistory>>()) } answers { firstArg<List<DailySalesHistory>>() }
        every {
            dailyRepo.findBySapAccountCodeInAndSalesDateStartingWith(listOf("1000077"), "202601")
        } returns existingRows
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
    }

    @Test
    @DisplayName("월별 재합산은 이번 실행 처리분 밖의 기존 월 row (ledger 포함) 도 합산에 포함")
    fun monthlyAggregateIncludesRowsOutsideThisRun() {
        // 이번 실행은 20260116 1건만 수신 — 월 재조회에는 기존 20260115 row (ledger 30 포함) 도 있음.
        every {
            ororaRepo.findBySalesDateStartingWithAndSapAccountCodeBetween(any(), any(), any())
        } returns listOf(ororaRow("0001000077", "20260116", "500", "100"))
        every { accountRepo.findByExternalKeyIn(any()) } returns listOf(account(5, "1000077"))
        every { dailyRepo.findByExternalKeyIn(listOf("100007720260116")) } returns emptyList()
        every { dailyRepo.saveAll(any<List<DailySalesHistory>>()) } answers { firstArg<List<DailySalesHistory>>() }
        every {
            dailyRepo.findBySapAccountCodeInAndSalesDateStartingWith(listOf("1000077"), "202601")
        } returns listOf(
            dailyRow("1000077", "100007720260115", 1000.0, 200.0, ledger = 30.0),
            dailyRow("1000077", "100007720260116", 500.0, 100.0),
        )
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
        assertThat(m.totalLedgerAmount).isEqualByComparingTo(BigDecimal.valueOf(30.0))
    }

    @Test
    @DisplayName("기존 운영 입력 월 row 유지: 재합산은 합계 3컬럼만 대입하고 목표/비고/마감은 미변경")
    fun monthlyAggregatePreservesOperationalColumns() {
        val existingMonthly = MonthlySalesHistory(
            sapAccountCode = "1000077",
            salesYear = SalesYear.Y2026,
            salesMonth = SalesMonth.M01,
            externalkeyC = "1000077202601",
            abcClosingSumAmount = 999.0, // 이전 합계 — 재합산으로 대체돼야 함
        )
        every {
            ororaRepo.findBySalesDateStartingWithAndSapAccountCodeBetween(any(), any(), any())
        } returns listOf(ororaRow("0001000077", "20260115", "1000", "200"))
        every { accountRepo.findByExternalKeyIn(any()) } returns listOf(account(5, "1000077"))
        every { dailyRepo.findByExternalKeyIn(any()) } returns emptyList()
        every { dailyRepo.saveAll(any<List<DailySalesHistory>>()) } answers { firstArg<List<DailySalesHistory>>() }
        every {
            dailyRepo.findBySapAccountCodeInAndSalesDateStartingWith(listOf("1000077"), "202601")
        } returns listOf(dailyRow("1000077", "100007720260115", 1000.0, 200.0))
        every {
            monthlyRepo.findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(any(), any(), any())
        } returns listOf(existingMonthly)
        val savedMonthly = slot<List<MonthlySalesHistory>>()
        every { monthlyRepo.saveAll(capture(savedMonthly)) } answers { savedMonthly.captured }

        processor.process("202601", SalesYear.Y2026, SalesMonth.M01, "0001000000", "0001001999")

        val m = savedMonthly.captured.first()
        assertThat(m).isSameAs(existingMonthly) // 신규 생성이 아니라 기존 row 갱신
        assertThat(m.abcClosingSumAmount).isEqualTo(1000.0)
        assertThat(m.shipClosingSumAmount).isEqualTo(200.0)
    }
}
