package com.otoki.powersales.schedule.repository

import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import java.time.LocalDate
import java.time.LocalDateTime

interface TeamMemberScheduleRepositoryCustom {

    fun updateCommuteLogId(id: Long, commuteLogId: String)

    fun updateSafetyCheckData(
        id: Long,
        equipment1: String?,
        equipment2: String?,
        equipment3: String?,
        equipment4: String?,
        equipment5: String?,
        equipment6: String?,
        equipment7: String?,
        equipment8: String?,
        equipment9: String?,
        yesChkCnt: Double?,
        noChkCnt: Double?,
        startTime: LocalDateTime?,
        completeTime: LocalDateTime?,
        precaution: String?,
        precautionChk: Double?,
        traversalFlag: String?
    )

    fun findByEmployeeIdAndWorkingDate(employeeId: Long, workingDate: LocalDate): List<TeamMemberSchedule>

    fun findMonthlyByEmployeeIds(employeeIds: List<Long>, from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    fun findMonthlyByAccountIds(accountIds: List<Int>, from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    fun findActiveByEmployeeIdAndDate(employeeId: Long, workingDate: LocalDate): List<TeamMemberSchedule>

    fun deleteAnnualLeaveByEmployeeIdAndDateRange(employeeId: Long, from: LocalDate, to: LocalDate): Long

    fun deleteFutureWorkSchedulesByEmployeeId(employeeId: Long, fromDate: LocalDate): Long

    fun findAnnualLeaveByDateRange(from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    fun findAnnualLeaveByDateRangeAndEmployeeIds(from: LocalDate, to: LocalDate, employeeIds: List<Long>): List<TeamMemberSchedule>

    fun findDistinctAccountIdsByEmployeeIdAndDateRange(employeeId: Long, fromDate: LocalDate, toDate: LocalDate): List<Int>

    fun findIntegrationScheduleRecords(employeeIds: List<Long>, from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    fun findWorkSchedulesByEmployeeAndAccountAndMonth(employeeId: Long, accountId: Int, from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    fun countWorkSchedulesByEmployeeAndDateAndWorkingType(employeeId: Long, workingDate: LocalDate): Int
}
