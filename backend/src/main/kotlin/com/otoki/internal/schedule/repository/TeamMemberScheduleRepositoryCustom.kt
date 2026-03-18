package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.TeamMemberSchedule
import java.time.LocalDate

interface TeamMemberScheduleRepositoryCustom {

    fun updateCommuteLogId(sfid: String, commuteLogId: String)

    fun findMonthlyByEmployeeIds(employeeIds: List<String>, from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    fun findMonthlyByAccountIds(accountIds: List<Int>, from: LocalDate, to: LocalDate): List<TeamMemberSchedule>

    fun findActiveByEmployeeIdAndDate(employeeId: String, workingDate: LocalDate): List<TeamMemberSchedule>
}
