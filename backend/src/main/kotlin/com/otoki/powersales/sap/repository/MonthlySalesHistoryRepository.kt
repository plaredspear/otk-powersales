package com.otoki.powersales.sap.repository

import com.otoki.powersales.sap.entity.Account
import com.otoki.powersales.sap.entity.MonthlySalesHistory
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

    fun findByExternalkeyCIn(externalkeyCs: List<String>): List<MonthlySalesHistory>

    fun findByAccountInAndSalesYearIn(
        accounts: List<Account>,
        salesYears: List<String>
    ): List<MonthlySalesHistory>
}
