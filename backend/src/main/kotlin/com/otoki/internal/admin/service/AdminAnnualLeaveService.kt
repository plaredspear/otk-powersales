package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.response.AnnualLeaveDayDto
import com.otoki.internal.admin.dto.response.EmployeeAnnualLeaveDto
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.schedule.repository.TeamMemberScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class AdminAnnualLeaveService(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val userRepository: UserRepository
) {

    fun getSummary(yearMonth: String, orgCode: String?): List<EmployeeAnnualLeaveDto> {
        val ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"))
        val from = ym.atDay(1)
        val to = ym.atEndOfMonth()

        val schedules = if (orgCode != null) {
            val users = userRepository.findByOrgName(orgCode)
            if (users.isEmpty()) return emptyList()
            val employeeIds = users.map { it.employeeId }
            teamMemberScheduleRepository.findAnnualLeaveByDateRangeAndEmployeeIds(from, to, employeeIds)
        } else {
            teamMemberScheduleRepository.findAnnualLeaveByDateRange(from, to)
        }

        if (schedules.isEmpty()) return emptyList()

        val employeeIds = schedules.mapNotNull { it.employeeId }.distinct()
        val userMap = userRepository.findByEmployeeIdIn(employeeIds).associateBy { it.employeeId }

        return schedules
            .groupBy { it.employeeId ?: "" }
            .filter { it.key.isNotBlank() }
            .map { (employeeId, employeeSchedules) ->
                val user = userMap[employeeId]
                val days = employeeSchedules
                    .sortedBy { it.workingDate }
                    .map { schedule ->
                        AnnualLeaveDayDto(
                            date = schedule.workingDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "",
                            attendTypeName = schedule.workingType ?: "연차"
                        )
                    }
                EmployeeAnnualLeaveDto(
                    employeeId = employeeId,
                    employeeName = user?.name ?: "",
                    orgName = user?.orgName ?: "",
                    annualLeaveDays = days,
                    totalCount = days.size
                )
            }
            .sortedBy { it.employeeId }
    }
}
