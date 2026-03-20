package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.response.AnnualLeaveDayDto
import com.otoki.internal.admin.dto.response.EmployeeAnnualLeaveDto
import com.otoki.internal.sap.repository.EmployeeRepository
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
    private val employeeRepository: EmployeeRepository
) {

    fun getSummary(yearMonth: String, orgCode: String?): List<EmployeeAnnualLeaveDto> {
        val ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"))
        val from = ym.atDay(1)
        val to = ym.atEndOfMonth()

        val schedules = if (orgCode != null) {
            val users = employeeRepository.findByOrgName(orgCode)
            if (users.isEmpty()) return emptyList()
            val employeeIds = users.map { it.id }
            teamMemberScheduleRepository.findAnnualLeaveByDateRangeAndEmployeeIds(from, to, employeeIds)
        } else {
            teamMemberScheduleRepository.findAnnualLeaveByDateRange(from, to)
        }

        if (schedules.isEmpty()) return emptyList()

        val employeeIds = schedules.mapNotNull { it.employeeId }.distinct()
        val userMap = employeeRepository.findAllById(employeeIds).associateBy { it.id }

        return schedules
            .groupBy { it.employeeId ?: 0L }
            .filter { it.key != 0L }
            .map { (employeeId, employeeSchedules) ->
                val employee = userMap[employeeId]
                val days = employeeSchedules
                    .sortedBy { it.workingDate }
                    .map { schedule ->
                        AnnualLeaveDayDto(
                            date = schedule.workingDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "",
                            attendTypeName = schedule.workingType ?: "연차"
                        )
                    }
                EmployeeAnnualLeaveDto(
                    employeeNumber = employee?.employeeNumber ?: "",
                    employeeName = employee?.name ?: "",
                    orgName = employee?.orgName ?: "",
                    annualLeaveDays = days,
                    totalCount = days.size
                )
            }
            .sortedBy { it.employeeNumber }
    }
}
