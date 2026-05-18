package com.otoki.powersales.sales.repository

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.sales.enums.SalesMonth
import com.otoki.powersales.sales.enums.SalesYear
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 월매출 이력 Repository
 */
interface MonthlySalesHistoryRepository : JpaRepository<MonthlySalesHistory, Long> {

    fun findBySalesYearAndSalesMonth(salesYear: SalesYear, salesMonth: SalesMonth): List<MonthlySalesHistory>

    fun findBySalesYearAndSalesMonthAndAccountIn(
        salesYear: SalesYear,
        salesMonth: SalesMonth,
        accounts: List<Account>
    ): List<MonthlySalesHistory>

    fun findByExternalkeyC(externalkeyC: String): MonthlySalesHistory?

    fun findByExternalkeyCIn(externalkeyCs: List<String>): List<MonthlySalesHistory>

    fun findByAccountInAndSalesYearIn(
        accounts: List<Account>,
        salesYears: List<SalesYear>
    ): List<MonthlySalesHistory>

    fun findBySalesYearAndSalesMonthInAndAccountIn(
        salesYear: SalesYear,
        salesMonths: List<SalesMonth>,
        accounts: List<Account>
    ): List<MonthlySalesHistory>
}
