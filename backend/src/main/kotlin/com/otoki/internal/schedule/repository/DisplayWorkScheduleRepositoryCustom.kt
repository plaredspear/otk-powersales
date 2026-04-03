package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.DisplayWorkSchedule
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface DisplayWorkScheduleRepositoryCustom {

    fun findDistinctAccountIdsByEmployeeIdAndStartDateBetween(
        employeeId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Int>

    fun findDistinctStartDatesByEmployeeIdAndDateBetween(
        employeeId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<LocalDate>

    fun findByEmployeeIdInAndNotDeleted(employeeIds: List<Long>): List<DisplayWorkSchedule>

    fun findDistinctAccountIdsByEmployeeIdAndDateRange(employeeId: Long, fromDate: LocalDate, toDate: LocalDate): List<Int>

    fun findScheduleList(
        employeeCode: String?,
        accountIds: List<Int>?,
        confirmed: Boolean?,
        typeOfWork3: String?,
        startDateFrom: LocalDate?,
        startDateTo: LocalDate?,
        pageable: Pageable
    ): Page<DisplayWorkSchedule>

    fun findByEmployeeAndStartDate(employeeId: Long, startDate: LocalDate): List<DisplayWorkSchedule>

    fun findByEmployeeAndAccountAndStartDate(employeeId: Long, accountId: Int, startDate: LocalDate): DisplayWorkSchedule?

    fun findByEmployeeAndStartDateBetween(employeeId: Long, start: LocalDate, end: LocalDate): List<DisplayWorkSchedule>

    fun findByEmployeeIdsAndAccountIds(employeeIds: List<Long>, accountIds: List<Int>): List<DisplayWorkSchedule>

    fun findConfirmedByDateRangeAndAccountIds(monthEnd: LocalDate, monthStart: LocalDate, accountIds: List<Int>): List<DisplayWorkSchedule>

    fun existsConfirmedByEmployeeAndAccountAndDate(employeeId: Long, accountId: Int, workingDate: LocalDate): Boolean
}
