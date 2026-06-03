package com.otoki.powersales.schedule.service

import com.otoki.powersales.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.sales.service.MonthlySalesHistoryQueryGateway
import com.otoki.powersales.sales.service.MonthlySalesRow
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
    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway = mockk()

    private val service = TeamMemberScheduleSearchService(expander, queryFactory, monthlySalesHistoryGateway)

    private fun row(accountId: Int?, externalKey: String? = null): TeamMemberScheduleRow =
        TeamMemberScheduleRow(
            year = null,
            month = null,
            name = null,
            accountId = accountId,
            accountExternalKey = externalKey,
            accountBranchName = null,
            accountName = null,
            employeeOrgName = null,
            employeeCode = null,
            employeeJikwee = null,
            employeeName = null,
            workingCategory1 = null,
            workingCategory3 = null,
            workingCategory4 = null,
            workingCategory5 = null,
            numberOfInputs = null,
            equivalentNumberOfWorkingDays = null,
            convertedHeadcount = null,
        )

    // closingAmountSum = (abc1+abc2+abc3+abc4) + (ship1+ship2+ship3+ship4) 를 게이트웨이가 산출한 결과.
    // 본 테스트는 합계만 검증하므로 abc1 + ship1 을 합계로 표현.
    // SF 정합(AccountId__c 기준 집계) — account_id 기반 조회 결과는 accountId 가 채워진다.
    private fun salesRow(accountId: Long, sapCode: String = "S001", abc1: Long = 0L, ship1: Long = 0L) =
        MonthlySalesRow(
            sapAccountCode = sapCode,
            salesDate = "",
            closingAmountSum = BigDecimal(abc1 + ship1),
            accountId = accountId,
            abcClosingAmount1 = BigDecimal(abc1),
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
        // SF 정합 — account_id(AccountId__c) 기준 조회. accountId 로 합산/divider.
        every { monthlySalesHistoryGateway.findBySalesDatesByAccountId(any(), any()) } returns listOf(
            salesRow(accountId = 1L, abc1 = 100, ship1 = 200),  // sum 300
            salesRow(accountId = 1L, abc1 = 500, ship1 = 100),  // sum 600
            salesRow(accountId = 1L, abc1 = 0, ship1 = 900),    // sum 900
        )

        val result = service.computeSixMonthAverageSales(listOf(row(1, "S001")), "2026", "3")

        // 3개월 합 (300 + 600 + 900) = 1800, divider = 3 → 600
        assertThat(result[1L]).isEqualByComparingTo(BigDecimal("600"))
    }

    @Test
    @DisplayName("accountId null Account → 매출 조회 안 함 + empty map (SF AccountId__c IN 정합)")
    fun nullAccountIdReturnsEmpty() {
        val result = service.computeSixMonthAverageSales(listOf(row(accountId = null)), "2026", "3")
        assertThat(result).isEmpty()
    }
}
