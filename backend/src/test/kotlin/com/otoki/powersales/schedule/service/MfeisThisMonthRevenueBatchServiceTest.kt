package com.otoki.powersales.schedule.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.sales.enums.SalesMonth
import com.otoki.powersales.sales.enums.SalesYear
import com.otoki.powersales.sales.repository.MonthlySalesHistoryRepository
import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

@DisplayName("MfeisThisMonthRevenueBatchService — MFEIS this_month_amount 월간 일괄 갱신 (#680 §5.2)")
class MfeisThisMonthRevenueBatchServiceTest {

    private val mfeisRepository: MonthlyFemaleEmployeeIntegrationScheduleRepository = mockk(relaxed = true)
    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository = mockk()

    private val chunkSize = 200
    private lateinit var service: MfeisThisMonthRevenueBatchService

    @BeforeEach
    fun setUp() {
        service = MfeisThisMonthRevenueBatchService(
            mfeisRepository = mfeisRepository,
            monthlySalesHistoryRepository = monthlySalesHistoryRepository,
            chunkSize = chunkSize,
        )
    }

    @Test
    @DisplayName("양수 필터 — 음수/0 매출 월은 합산/divider 모두 제외 (legacy 동등)")
    fun positiveAmountFilter() {
        val targetYm = YearMonth.of(2026, 4)
        val account = account(id = 1)
        val mfeis = mfeisRow(id = 100L, account = account, currentAmount = BigDecimal.ZERO)

        every { mfeisRepository.findByYearAndMonthAndWorkingCategory5Containing("2026", "04", "%상시%") } returns listOf(mfeis)
        every { monthlySalesHistoryRepository.findByAccountInAndSalesYearIn(any(), any()) } returns listOf(
            history(account, SalesYear.Y2025, SalesMonth.M11, 100_000.0),
            history(account, SalesYear.Y2025, SalesMonth.M12, 200_000.0),
            history(account, SalesYear.Y2026, SalesMonth.M01, 0.0),        // 제외
            history(account, SalesYear.Y2026, SalesMonth.M02, -50_000.0),  // 제외
            history(account, SalesYear.Y2026, SalesMonth.M03, 300_000.0),
            history(account, SalesYear.Y2026, SalesMonth.M04, 400_000.0),
        )
        val saved = slot<MonthlyFemaleEmployeeIntegrationSchedule>()
        every { mfeisRepository.save(capture(saved)) } answers { saved.captured }

        service.runMonthly(targetYm)

        // (100_000 + 200_000 + 300_000 + 400_000) / 4 = 250_000
        assertThat(saved.captured.thisMonthAmount).isEqualTo(BigDecimal("250000"))
        verify(exactly = 1) { mfeisRepository.save(any()) }
    }

    @Test
    @DisplayName("모든 매출이 0/음수 — divider=0 처리로 thisMonthAmount=0")
    fun allZeroAmountsResultInZero() {
        val targetYm = YearMonth.of(2026, 4)
        val account = account(id = 1)
        val mfeis = mfeisRow(id = 100L, account = account, currentAmount = BigDecimal.ONE)

        every { mfeisRepository.findByYearAndMonthAndWorkingCategory5Containing(any(), any(), any()) } returns listOf(mfeis)
        every { monthlySalesHistoryRepository.findByAccountInAndSalesYearIn(any(), any()) } returns listOf(
            history(account, SalesYear.Y2025, SalesMonth.M11, 0.0),
            history(account, SalesYear.Y2026, SalesMonth.M01, -100.0),
        )
        val saved = slot<MonthlyFemaleEmployeeIntegrationSchedule>()
        every { mfeisRepository.save(capture(saved)) } answers { saved.captured }

        service.runMonthly(targetYm)

        assertThat(saved.captured.thisMonthAmount).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    @DisplayName("변경 없는 row 는 save 호출 안 함 — current == new 일 때 skip")
    fun unchangedRowSkipsSave() {
        val targetYm = YearMonth.of(2026, 4)
        val account = account(id = 1)
        val mfeis = mfeisRow(id = 100L, account = account, currentAmount = BigDecimal("150000"))

        every { mfeisRepository.findByYearAndMonthAndWorkingCategory5Containing(any(), any(), any()) } returns listOf(mfeis)
        every { monthlySalesHistoryRepository.findByAccountInAndSalesYearIn(any(), any()) } returns listOf(
            history(account, SalesYear.Y2025, SalesMonth.M11, 100_000.0),
            history(account, SalesYear.Y2025, SalesMonth.M12, 200_000.0),
        )

        service.runMonthly(targetYm)

        verify(exactly = 0) { mfeisRepository.save(any()) }
    }

    @Test
    @DisplayName("빈 추출 결과 — save 0건 + history 조회 0건")
    fun emptyTargetsSkipsAllProcessing() {
        every { mfeisRepository.findByYearAndMonthAndWorkingCategory5Containing(any(), any(), any()) } returns emptyList()

        service.runMonthly(YearMonth.of(2026, 4))

        verify(exactly = 0) { mfeisRepository.save(any()) }
        verify(exactly = 0) { monthlySalesHistoryRepository.findByAccountInAndSalesYearIn(any(), any()) }
    }

    @Test
    @DisplayName("chunk 분할 — chunkSize 200 초과 시 청크 단위 history 조회")
    fun chunkedHistoryLookup() {
        val targetYm = YearMonth.of(2026, 4)
        val rows = (1..250).map { mfeisRow(id = it.toLong(), account = account(id = it), currentAmount = BigDecimal.ZERO) }
        every { mfeisRepository.findByYearAndMonthAndWorkingCategory5Containing(any(), any(), any()) } returns rows
        every { monthlySalesHistoryRepository.findByAccountInAndSalesYearIn(any(), any()) } returns emptyList()

        service.runMonthly(targetYm)

        // 250 → 2 chunk (200, 50)
        verify(exactly = 2) { monthlySalesHistoryRepository.findByAccountInAndSalesYearIn(any(), any()) }
    }

    // -- helpers --

    private fun account(id: Int): Account = Account(id = id, sfid = "ACC$id")

    private fun mfeisRow(
        id: Long,
        account: Account,
        currentAmount: BigDecimal?,
    ): MonthlyFemaleEmployeeIntegrationSchedule = MonthlyFemaleEmployeeIntegrationSchedule(
        id = id,
        year = "2026",
        month = "04",
        workingCategory5 = "상시",
        account = account,
        thisMonthAmount = currentAmount,
    )

    private fun history(
        account: Account,
        salesYear: SalesYear,
        salesMonth: SalesMonth,
        amount: Double,
    ): MonthlySalesHistory = MonthlySalesHistory(
        account = account,
        salesYear = salesYear,
        salesMonth = salesMonth,
        abcClosingAmount1 = amount,
    )
}
