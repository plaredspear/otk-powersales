package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.TeamMemberSchedule
import java.time.LocalDate

interface TeamMemberScheduleRepositoryCustom {

    fun updateCommuteLogId(sfid: String, commuteLogId: String)

    fun findMonthlyByEmployeeIds(employeeIds: List<Long>, from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    fun findMonthlyByAccountIds(accountIds: List<Int>, from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    fun findActiveByEmployeeIdAndDate(employeeId: Long, workingDate: LocalDate): List<TeamMemberSchedule>

    fun deleteAnnualLeaveByEmployeeIdAndDateRange(employeeId: Long, from: LocalDate, to: LocalDate): Long

    fun findAnnualLeaveByDateRange(from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    fun findAnnualLeaveByDateRangeAndEmployeeIds(from: LocalDate, to: LocalDate, employeeIds: List<Long>): List<TeamMemberSchedule>

    fun findDistinctAccountIdsByEmployeeIdAndDateRange(employeeId: Long, fromDate: LocalDate, toDate: LocalDate): List<Int>
}
