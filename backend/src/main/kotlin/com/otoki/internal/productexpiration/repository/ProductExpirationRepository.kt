package com.otoki.internal.productexpiration.repository

import com.otoki.internal.productexpiration.entity.ProductExpiration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ProductExpirationRepository : JpaRepository<ProductExpiration, Int> {

    fun countByEmployeeIdAndAlarmDate(employeeId: Long, alarmDate: LocalDate): Long

    fun findByEmployeeIdAndExpirationDateBetweenOrderByExpirationDateAsc(
        employeeId: Long, fromDate: LocalDate, toDate: LocalDate
    ): List<ProductExpiration>

    fun findByEmployeeIdAndAccountCodeAndExpirationDateBetweenOrderByExpirationDateAsc(
        employeeId: Long, accountCode: String, fromDate: LocalDate, toDate: LocalDate
    ): List<ProductExpiration>

    fun findBySeqInAndEmployeeId(seqs: List<Int>, employeeId: Long): List<ProductExpiration>
}
