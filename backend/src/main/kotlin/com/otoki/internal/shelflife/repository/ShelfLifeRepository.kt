package com.otoki.internal.shelflife.repository

import com.otoki.internal.shelflife.entity.ShelfLife
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ShelfLifeRepository : JpaRepository<ShelfLife, Int> {

    fun countByEmployeeNumberAndAlarmDate(employeeNumber: String, alarmDate: LocalDate): Long

    fun findByEmployeeNumberAndExpirationDateBetweenOrderByExpirationDateAsc(
        employeeNumber: String, fromDate: LocalDate, toDate: LocalDate
    ): List<ShelfLife>

    fun findByEmployeeNumberAndAccountCodeAndExpirationDateBetweenOrderByExpirationDateAsc(
        employeeNumber: String, accountCode: String, fromDate: LocalDate, toDate: LocalDate
    ): List<ShelfLife>

    fun findBySeqInAndEmployeeNumber(seqs: List<Int>, employeeNumber: String): List<ShelfLife>
}
