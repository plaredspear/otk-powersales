package com.otoki.powersales.schedule.service.internal

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.sales.enums.SalesMonth
import com.otoki.powersales.sales.enums.SalesYear
import com.otoki.powersales.sales.repository.MonthlySalesHistoryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("LastMonthRevenueLookup 테스트")
class LastMonthRevenueLookupTest {

    private val repository: MonthlySalesHistoryRepository = mockk(relaxUnitFun = true)
    private val lookup = LastMonthRevenueLookup(repository)

    private val today = LocalDate.of(2026, 5, 15)
    private val expectedSalesYear = SalesYear.Y2026
    private val expectedSalesMonth = SalesMonth.M04

    private fun account(id: Int): Account = mockk {
        every { this@mockk.id } returns id
    }

    private fun history(account: Account?, result: BigDecimal?): MonthlySalesHistory = mockk {
        every { this@mockk.account } returns account
        every { this@mockk.lastMonthResults } returns result
    }

    @Nested
    @DisplayName("forAccounts — 일괄 lookup")
    inner class ForAccounts {

        @Test
        @DisplayName("Account 3개 + 매출 2건만 존재 → 매출 없는 account 는 entry 부재, 절단 적용")
        fun returnsMapWithOnlyMatchedAccountsScaled() {
            val a1 = account(1)
            val a2 = account(2)
            val a3 = account(3)
            every {
                repository.findBySalesYearAndSalesMonthAndAccountIn(expectedSalesYear, expectedSalesMonth, listOf(a1, a2, a3))
            } returns listOf(
                history(a1, BigDecimal("12345.67")),
                history(a2, BigDecimal("1000")),
                // a3 매출 row 없음
            )

            val result = lookup.forAccounts(listOf(a1, a2, a3), today = today)

            assertThat(result).containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    1 to BigDecimal("12346"),  // 12345.67 → setScale(0, HALF_UP) → 12346
                    2 to BigDecimal("1000"),
                )
            )
        }

        @Test
        @DisplayName("accounts.isEmpty() → emptyMap 반환 + Repository 호출 안 함")
        fun emptyAccountsReturnsEmptyMapWithoutRepositoryCall() {
            val result = lookup.forAccounts(emptyList(), today = today)

            assertThat(result).isEmpty()
            verify(exactly = 0) {
                repository.findBySalesYearAndSalesMonthAndAccountIn(any(), any(), any())
            }
        }

        @Test
        @DisplayName("today 가 SalesYear enum 범위 밖 → emptyMap 반환 + Repository 호출 안 함")
        fun previousMonthOutsideEnumRangeReturnsEmptyMap() {
            // today=2018-01-15 → lastMonth=2017-12 → SalesYear.fromValueOrNull(\"2017\") = null
            val result = lookup.forAccounts(listOf(account(1)), today = LocalDate.of(2018, 1, 15))

            assertThat(result).isEmpty()
            verify(exactly = 0) {
                repository.findBySalesYearAndSalesMonthAndAccountIn(any(), any(), any())
            }
        }

        @Test
        @DisplayName("MonthlySalesHistory.account == null row 는 필터로 제외")
        fun rowWithNullAccountIsFilteredOut() {
            val a1 = account(1)
            every {
                repository.findBySalesYearAndSalesMonthAndAccountIn(expectedSalesYear, expectedSalesMonth, listOf(a1))
            } returns listOf(
                history(a1, BigDecimal("500")),
                history(null, BigDecimal("999")),  // 필터 제외
            )

            val result = lookup.forAccounts(listOf(a1), today = today)

            assertThat(result).containsExactlyEntriesOf(mapOf(1 to BigDecimal("500")))
        }
    }

    @Nested
    @DisplayName("forAccount — 단건 lookup")
    inner class ForAccount {

        @Test
        @DisplayName("단건 매출 존재 → setScale(0, HALF_UP) 절단 반환")
        fun singleAccountWithRevenueReturnsScaledValue() {
            val a1 = account(1)
            every {
                repository.findBySalesYearAndSalesMonthAndAccountIn(expectedSalesYear, expectedSalesMonth, listOf(a1))
            } returns listOf(history(a1, BigDecimal("12345.67")))

            val result = lookup.forAccount(a1, today = today)

            assertThat(result).isEqualTo(BigDecimal("12346"))
        }

        @Test
        @DisplayName("account == null → null 반환 + Repository 호출 안 함")
        fun nullAccountReturnsNullWithoutRepositoryCall() {
            val result = lookup.forAccount(null, today = today)

            assertThat(result).isNull()
            verify(exactly = 0) {
                repository.findBySalesYearAndSalesMonthAndAccountIn(any(), any(), any())
            }
        }

        @Test
        @DisplayName("매출 row 부재 → null 반환")
        fun singleAccountWithoutRevenueReturnsNull() {
            val a1 = account(1)
            every {
                repository.findBySalesYearAndSalesMonthAndAccountIn(expectedSalesYear, expectedSalesMonth, listOf(a1))
            } returns emptyList()

            val result = lookup.forAccount(a1, today = today)

            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("today 인자 — default vs 명시")
    inner class TodayArgument {

        @Test
        @DisplayName("today=2026-02-15 → 직전월=2026-01 (leftPad '01' 정합)")
        fun previousMonthRespectsLeftPadConvention() {
            val a1 = account(1)
            every {
                repository.findBySalesYearAndSalesMonthAndAccountIn(SalesYear.Y2026, SalesMonth.M01, listOf(a1))
            } returns listOf(history(a1, BigDecimal("777")))

            val result = lookup.forAccount(a1, today = LocalDate.of(2026, 2, 15))

            assertThat(result).isEqualTo(BigDecimal("777"))
            verify(exactly = 1) {
                repository.findBySalesYearAndSalesMonthAndAccountIn(SalesYear.Y2026, SalesMonth.M01, listOf(a1))
            }
        }
    }
}
