package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.MonthlySalesHistory
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 월매출 이력 Repository
 */
interface MonthlySalesHistoryRepository : JpaRepository<MonthlySalesHistory, Long> {

    fun findBySalesYearAndSalesMonth(salesYear: String, salesMonth: String): List<MonthlySalesHistory>

    fun findBySalesYearAndSalesMonthAndAccountIn(
        salesYear: String,
        salesMonth: String,
        accounts: List<Account>
    ): List<MonthlySalesHistory>

    fun findByExternalkeyC(externalkeyC: String): MonthlySalesHistory?

    fun findByAccountInAndSalesYearIn(
        accounts: List<Account>,
        salesYears: List<String>
    ): List<MonthlySalesHistory>
}
