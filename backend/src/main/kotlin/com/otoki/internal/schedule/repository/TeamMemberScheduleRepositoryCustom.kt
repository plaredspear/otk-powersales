package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.TeamMemberSchedule
import java.time.LocalDate

interface TeamMemberScheduleRepositoryCustom {

    fun updateCommuteLogId(sfid: String, commuteLogId: String)

    fun findMonthlyByEmployeeNumbers(employeeNumbers: List<String>, from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    fun findMonthlyByAccountIds(accountIds: List<Int>, from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    fun findActiveByEmployeeNumberAndDate(employeeNumber: String, workingDate: LocalDate): List<TeamMemberSchedule>

    fun deleteAnnualLeaveByEmployeeNumberAndDateRange(employeeNumber: String, from: LocalDate, to: LocalDate): Long

    fun findAnnualLeaveByDateRange(from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    fun findAnnualLeaveByDateRangeAndEmployeeNumbers(from: LocalDate, to: LocalDate, employeeNumbers: List<String>): List<TeamMemberSchedule>

    fun findDistinctAccountIdsByEmployeeNumberAndDateRange(employeeNumber: String, fromDate: LocalDate, toDate: LocalDate): List<Int>
}
