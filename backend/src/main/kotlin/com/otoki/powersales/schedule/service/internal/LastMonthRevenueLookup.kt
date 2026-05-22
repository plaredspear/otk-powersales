package com.otoki.powersales.schedule.service.internal

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.sales.enums.SalesMonth
import com.otoki.powersales.sales.enums.SalesYear
import com.otoki.powersales.sales.repository.MonthlySalesHistoryRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * `DisplayWorkSchedule.lastMonthRevenue` 적재용 직전월 매출 lookup helper.
 *
 * Spec #784 — `AdminScheduleService` 의 `confirmUpload` / `createSchedule` / `updateSchedule`
 * 3곳이 인라인 중복 보유하던 lookup 패턴 (직전월 산출 + `MonthlySalesHistoryRepository`
 * fetch + `setScale(0, HALF_UP)` 절단 + null-guard) 을 단일 책임으로 추출.
 *
 * SF `LastMonthRevenue__c` 의 `double precision=18 scale=0` 정합 보존 — `setScale(0, HALF_UP)`
 * 로 소수점 절단. `today` 입력은 default `LocalDate.now()` (호출 측의 기존 동작 그대로 흡수;
 * 시계 주입은 별도 spec 책임).
 *
 * 호출 의도별 두 변형 제공:
 * - [forAccounts] — 일괄 (엑셀 import) 진입점. `Map<accountId, revenue>` 반환.
 * - [forAccount] — 단건 (CRUD) 진입점. `BigDecimal?` 반환.
 */
@Component
class LastMonthRevenueLookup(
    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository,
) {

    /**
     * 일괄 lookup — `accounts` 전체의 직전월 매출을 `account.id → revenue` Map 으로 반환.
     *
     * 매출이 없는 account 는 entry 부재 (호출 측에서 `map[id]?` null-safe 인용). `accounts.isEmpty()`
     * 또는 직전월 산출 실패 (`SalesYear/SalesMonth.fromValueOrNull` null) 시 `emptyMap()`
     * 반환 — Repository 호출 안 함. `MonthlySalesHistory.account == null` row 는 필터로 제외.
     */
    fun forAccounts(
        accounts: List<Account>,
        today: LocalDate = LocalDate.now(),
    ): Map<Int, BigDecimal> {
        if (accounts.isEmpty()) return emptyMap()
        val (salesYear, salesMonth) = resolvePreviousMonth(today) ?: return emptyMap()
        return monthlySalesHistoryRepository
            .findBySalesYearAndSalesMonthAndAccountIn(salesYear, salesMonth, accounts)
            .filter { it.account != null && it.lastMonthResults != null }
            .associate { it.account!!.id to it.lastMonthResults!!.setScale(0, RoundingMode.HALF_UP) }
    }

    /**
     * 단건 lookup — 단일 `account` 의 직전월 매출 반환.
     *
     * `account == null` 또는 직전월 산출 실패 또는 매출 row 부재 시 `null` 반환 — Repository 호출
     * 안 함 (account null) 또는 `firstOrNull() == null` 자연 흡수.
     */
    fun forAccount(
        account: Account?,
        today: LocalDate = LocalDate.now(),
    ): BigDecimal? {
        if (account == null) return null
        val (salesYear, salesMonth) = resolvePreviousMonth(today) ?: return null
        return monthlySalesHistoryRepository
            .findBySalesYearAndSalesMonthAndAccountIn(salesYear, salesMonth, listOf(account))
            .firstOrNull()
            ?.lastMonthResults
            ?.setScale(0, RoundingMode.HALF_UP)
    }

    private fun resolvePreviousMonth(today: LocalDate): Pair<SalesYear, SalesMonth>? {
        val lastMonth = today.minusMonths(1)
        val salesYear = SalesYear.fromValueOrNull(lastMonth.year.toString()) ?: return null
        val salesMonth = SalesMonth.fromValueOrNull(String.format("%02d", lastMonth.monthValue)) ?: return null
        return salesYear to salesMonth
    }
}
