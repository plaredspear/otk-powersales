package com.otoki.powersales.schedule.service.internal

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.sales.service.MonthlySalesHistoryQueryGateway
import com.otoki.powersales.domain.sales.service.MonthlySalesRow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("LastMonthRevenueLookup 테스트 — RDS `closingAmountSum` 정합")
class LastMonthRevenueLookupTest {

    private val monthlySalesHistoryGateway: MonthlySalesHistoryQueryGateway = mockk(relaxUnitFun = true)
    private val lookup = LastMonthRevenueLookup(monthlySalesHistoryGateway)

    private val today = LocalDate.of(2026, 5, 15)
    private val expectedSalesDate = "202604"

    private fun account(id: Long, externalKey: String?): Account = mockk {
        every { this@mockk.id } returns id
        every { this@mockk.externalKey } returns externalKey
    }

    private fun row(sapCode: String, abc1: Long = 0L, ship1: Long = 0L, salesDate: String = expectedSalesDate): MonthlySalesRow =
        MonthlySalesRow(
            sapAccountCode = sapCode,
            salesDate = salesDate,
            closingAmountSum = BigDecimal(abc1 + ship1),
            abcClosingAmount1 = BigDecimal(abc1),
            shipClosingAmount1 = BigDecimal(ship1),
        )

    @Nested
    @DisplayName("forAccounts — 일괄 lookup")
    inner class ForAccounts {

        @Test
        @DisplayName("Account 3개 + 매출 2건만 존재 → 매출 없는 account 는 entry 부재, ABC+Ship 합산 정합")
        fun returnsMapWithOnlyMatchedAccountsScaled() {
            val a1 = account(1, "S001")
            val a2 = account(2, "S002")
            val a3 = account(3, "S003")
            every {
                monthlySalesHistoryGateway.findBySalesDates(listOf(expectedSalesDate), setOf("S001", "S002", "S003"))
            } returns listOf(
                row("S001", abc1 = 7000, ship1 = 5346),     // 합 12346
                row("S002", abc1 = 1000, ship1 = 0),        // 합 1000
                // S003 row 없음
            )

            val result = lookup.forAccounts(listOf(a1, a2, a3), today = today)

            assertThat(result).containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    1L to BigDecimal("12346"),
                    2L to BigDecimal("1000"),
                )
            )
        }

        @Test
        @DisplayName("accounts.isEmpty() → emptyMap 반환 + gateway 호출 안 함")
        fun emptyAccountsReturnsEmptyMapWithoutGatewayCall() {
            val result = lookup.forAccounts(emptyList(), today = today)

            assertThat(result).isEmpty()
            verify(exactly = 0) { monthlySalesHistoryGateway.findBySalesDates(any(), any()) }
        }

        @Test
        @DisplayName("Account 들에 externalKey 가 모두 null → emptyMap 반환 + gateway 호출 안 함")
        fun accountsWithoutExternalKeyReturnsEmptyMap() {
            val a1 = account(1, externalKey = null)

            val result = lookup.forAccounts(listOf(a1), today = today)

            assertThat(result).isEmpty()
            verify(exactly = 0) { monthlySalesHistoryGateway.findBySalesDates(any(), any()) }
        }

        @Test
        @DisplayName("일부 Account 의 externalKey null → 해당 Account 제외 후 나머지로 조회")
        fun filtersOutAccountsWithoutExternalKey() {
            val a1 = account(1, "S001")
            val a2 = account(2, externalKey = null)
            every { monthlySalesHistoryGateway.findBySalesDates(listOf(expectedSalesDate), setOf("S001")) } returns
                listOf(row("S001", abc1 = 100, ship1 = 200))

            val result = lookup.forAccounts(listOf(a1, a2), today = today)

            assertThat(result).containsExactlyEntriesOf(mapOf(1L to BigDecimal("300")))
        }
    }

    @Nested
    @DisplayName("forAccount — 단건 lookup")
    inner class ForAccount {

        @Test
        @DisplayName("단건 매출 존재 → ABC+Ship 합 절단 반환")
        fun singleAccountWithRevenueReturnsScaledValue() {
            val a1 = account(1, "S001")
            every { monthlySalesHistoryGateway.findBySalesDates(listOf(expectedSalesDate), listOf("S001")) } returns
                listOf(row("S001", abc1 = 6000, ship1 = 6346))

            val result = lookup.forAccount(a1, today = today)

            assertThat(result).isEqualTo(BigDecimal("12346"))
        }

        @Test
        @DisplayName("account == null → null 반환 + gateway 호출 안 함")
        fun nullAccountReturnsNullWithoutGatewayCall() {
            val result = lookup.forAccount(null, today = today)

            assertThat(result).isNull()
            verify(exactly = 0) { monthlySalesHistoryGateway.findBySalesDates(any(), any()) }
        }

        @Test
        @DisplayName("account.externalKey == null → null 반환 + gateway 호출 안 함")
        fun nullExternalKeyReturnsNullWithoutGatewayCall() {
            val a1 = account(1, externalKey = null)

            val result = lookup.forAccount(a1, today = today)

            assertThat(result).isNull()
            verify(exactly = 0) { monthlySalesHistoryGateway.findBySalesDates(any(), any()) }
        }

        @Test
        @DisplayName("매출 row 부재 → null 반환")
        fun singleAccountWithoutRevenueReturnsNull() {
            val a1 = account(1, "S001")
            every { monthlySalesHistoryGateway.findBySalesDates(listOf(expectedSalesDate), listOf("S001")) } returns emptyList()

            val result = lookup.forAccount(a1, today = today)

            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("today 인자 — leftPad 정합")
    inner class TodayArgument {

        @Test
        @DisplayName("today=2026-02-15 → 직전월=`202601` (leftPad 6자)")
        fun previousMonthRespectsLeftPadConvention() {
            val a1 = account(1, "S001")
            every { monthlySalesHistoryGateway.findBySalesDates(listOf("202601"), listOf("S001")) } returns
                listOf(row("S001", abc1 = 777, salesDate = "202601"))

            val result = lookup.forAccount(a1, today = LocalDate.of(2026, 2, 15))

            assertThat(result).isEqualTo(BigDecimal("777"))
            verify(exactly = 1) { monthlySalesHistoryGateway.findBySalesDates(listOf("202601"), listOf("S001")) }
        }
    }
}
