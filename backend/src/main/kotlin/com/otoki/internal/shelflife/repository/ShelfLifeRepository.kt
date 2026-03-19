package com.otoki.internal.shelflife.repository

import com.otoki.internal.shelflife.entity.ShelfLife
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface ShelfLifeRepository : JpaRepository<ShelfLife, Int> {

    fun countByEmployeeIdAndAlarmDate(employeeId: Long, alarmDate: LocalDate): Long

    fun findByEmployeeIdAndExpirationDateBetweenOrderByExpirationDateAsc(
        employeeId: Long, fromDate: LocalDate, toDate: LocalDate
    ): List<ShelfLife>

    fun findByEmployeeIdAndAccountCodeAndExpirationDateBetweenOrderByExpirationDateAsc(
        employeeId: Long, accountCode: String, fromDate: LocalDate, toDate: LocalDate
    ): List<ShelfLife>

    fun findBySeqInAndEmployeeId(seqs: List<Int>, employeeId: Long): List<ShelfLife>
}
