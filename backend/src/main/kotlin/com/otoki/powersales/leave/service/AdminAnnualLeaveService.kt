package com.otoki.powersales.leave.service

import com.otoki.powersales.leave.dto.response.AnnualLeaveDayDto
import com.otoki.powersales.leave.dto.response.EmployeeAnnualLeaveDto
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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

        return schedules
            .groupBy { it.employee?.id ?: 0L }
            .filter { it.key != 0L }
            .map { (_, employeeSchedules) ->
                val employee = employeeSchedules.firstOrNull()?.employee
                val days = employeeSchedules
                    .sortedBy { it.workingDate }
                    .map { schedule ->
                        AnnualLeaveDayDto(
                            date = schedule.workingDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "",
                            attendTypeName = schedule.workingType?.displayName ?: "연차"
                        )
                    }
                EmployeeAnnualLeaveDto(
                    employeeCode = employee?.employeeCode ?: "",
                    employeeName = employee?.name ?: "",
                    orgName = employee?.orgName ?: "",
                    annualLeaveDays = days,
                    totalCount = days.size
                )
            }
            .sortedBy { it.employeeCode }
    }
}
