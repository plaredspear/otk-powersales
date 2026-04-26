package com.otoki.powersales.schedule.repository

import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
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

    /**
     * 사원의 오늘 유효한 확정 진열마스터 조회
     * 조건: confirmed=true, isDeleted!=true, startDate<=date, (endDate>=date OR endDate IS NULL), employee.id=employeeId
     */
    fun findConfirmedValidByEmployeeAndDate(employeeId: Long, date: LocalDate): List<DisplayWorkSchedule>

    /**
     * 복수 사원의 오늘 유효한 확정 진열마스터 조회
     */
    fun findConfirmedValidByEmployeeIdsAndDate(employeeIds: List<Long>, date: LocalDate): List<DisplayWorkSchedule>
}
