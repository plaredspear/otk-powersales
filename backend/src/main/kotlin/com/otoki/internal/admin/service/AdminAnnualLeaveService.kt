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
            val employeeNumbers = users.map { it.employeeNumber }
            teamMemberScheduleRepository.findAnnualLeaveByDateRangeAndEmployeeNumbers(from, to, employeeNumbers)
        } else {
            teamMemberScheduleRepository.findAnnualLeaveByDateRange(from, to)
        }

        if (schedules.isEmpty()) return emptyList()

        val employeeNumbers = schedules.mapNotNull { it.employeeNumber }.distinct()
        val userMap = userRepository.findByEmployeeNumberIn(employeeNumbers).associateBy { it.employeeNumber }

        return schedules
            .groupBy { it.employeeNumber ?: "" }
            .filter { it.key.isNotBlank() }
            .map { (employeeNumber, employeeSchedules) ->
                val user = userMap[employeeNumber]
                val days = employeeSchedules
                    .sortedBy { it.workingDate }
                    .map { schedule ->
                        AnnualLeaveDayDto(
                            date = schedule.workingDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "",
                            attendTypeName = schedule.workingType ?: "연차"
                        )
                    }
                EmployeeAnnualLeaveDto(
                    employeeNumber = employeeNumber,
                    employeeName = user?.name ?: "",
                    orgName = user?.orgName ?: "",
                    annualLeaveDays = days,
                    totalCount = days.size
                )
            }
            .sortedBy { it.employeeNumber }
    }
}
