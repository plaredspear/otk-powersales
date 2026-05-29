package com.otoki.powersales.schedule.service

import com.otoki.orora.entity.OroraMonthlySalesHistory
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.sales.service.OroraMonthlySalesHistoryQueryGateway
import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.querydsl.jpa.impl.JPAQueryFactory
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@DisplayName("TeamMemberScheduleSearchService — 6개월 평균 ClosingAmountSum 정합 보호")
class TeamMemberScheduleSearchServiceTest {

    private val expander: BranchCodeExpander = mockk()
    private val queryFactory: JPAQueryFactory = mockk()
    private val ororaGateway: OroraMonthlySalesHistoryQueryGateway = mockk()

    private val service = TeamMemberScheduleSearchService(expander, queryFactory, ororaGateway)

    private fun account(id: Int, externalKey: String?): Account = mockk {
        every { this@mockk.id } returns id
        every { this@mockk.externalKey } returns externalKey
    }

    private fun schedule(account: Account): MonthlyFemaleEmployeeIntegrationSchedule = mockk {
        every { this@mockk.account } returns account
    }

    private fun row(sapCode: String, salesDate: String, abc1: Long = 0L, ship1: Long = 0L) =
        OroraMonthlySalesHistory(
            sapAccountCode = sapCode,
            salesDate = salesDate,
            abcClosingAmount1 = BigDecimal(abc1),
            shipClosingAmount1 = BigDecimal(ship1),
        )

    @Test
    @DisplayName("당월 검색 — 6개월 범위 [-6, -1] (legacy SF L143-150 정합)")
    fun sixMonthRangeForCurrentMonth() {
        val today = LocalDate.of(2026, 5, 15)
        val (start, end) = service.sixMonthRange("2026", "5", today)
        assertThat(start).isEqualTo(YearMonth.of(2025, 11))
        assertThat(end).isEqualTo(YearMonth.of(2026, 4))
    }

    @Test
    @DisplayName("과거월 검색 — 6개월 범위 [-5, 0] (legacy SF L143-150 정합)")
    fun sixMonthRangeForPastMonth() {
        val today = LocalDate.of(2026, 5, 15)
        val (start, end) = service.sixMonthRange("2026", "3", today)
        assertThat(start).isEqualTo(YearMonth.of(2025, 10))
        assertThat(end).isEqualTo(YearMonth.of(2026, 3))
    }

    @Test
    @DisplayName("6개월 평균 ClosingAmountSum 산출 — ABC+Ship 합산 / divider = 데이터 존재 월 수")
    fun computeAverageSumsAbcAndShipDividesByExistingMonths() {
        val acc = account(1, "S001")
        every { ororaGateway.findBySalesDates(any(), any()) } returns listOf(
            row("S001", "202510", abc1 = 100, ship1 = 200),  // sum 300
            row("S001", "202511", abc1 = 500, ship1 = 100),  // sum 600
            row("S001", "202512", abc1 = 0, ship1 = 900),    // sum 900
        )

        val result = service.computeSixMonthAverageSales(listOf(schedule(acc)), "2026", "3")

        // 3개월 합 (300 + 600 + 900) = 1800, divider = 3 → 600
        assertThat(result[1L]).isEqualByComparingTo(BigDecimal("600"))
    }

    @Test
    @DisplayName("externalKey null Account → ORORA 조회 안 함 + empty map")
    fun nullExternalKeyReturnsEmpty() {
        val acc = account(1, externalKey = null)
        val result = service.computeSixMonthAverageSales(listOf(schedule(acc)), "2026", "3")
        assertThat(result).isEmpty()
    }
}
