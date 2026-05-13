package com.otoki.powersales.schedule.dto.response

import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.schedule.entity.TeamMemberSchedule

data class TeamMemberDto(
    val employeeId: Long,
    val employeeCode: String,
    val name: String
) {
    companion object {
        fun from(employee: Employee): TeamMemberDto = TeamMemberDto(
            employeeId = employee.id,
            employeeCode = employee.employeeCode,
            name = employee.name
        )
    }
}

data class TeamScheduleAccountDto(
    val accountId: Int,
    val externalKey: String,
    val name: String
) {
    companion object {
        fun from(account: Account): TeamScheduleAccountDto = TeamScheduleAccountDto(
            accountId = account.id,
            externalKey = account.externalKey ?: "",
            name = account.name ?: ""
        )
    }
}

data class TeamScheduleDto(
    val id: Long,
    val employeeCode: String,
    val employeeName: String,
    val workingDate: String,
    val workingType: String,
    val workingCategory1: String?,
    val workingCategory2: String?,
    val workingCategory3: String?,
    val accountId: Int?,
    val accountName: String?,
    val accountExternalKey: String?,
    val isClockIn: Boolean
) {
    companion object {
        fun from(schedule: TeamMemberSchedule): TeamScheduleDto {
            return TeamScheduleDto(
                id = schedule.id,
                employeeCode = schedule.employee?.employeeCode ?: "",
                employeeName = schedule.employee?.name ?: "",
                workingDate = schedule.workingDate?.toString() ?: "",
                workingType = schedule.workingType ?: "",
                workingCategory1 = schedule.workingCategory1,
                workingCategory2 = schedule.workingCategory2,
                workingCategory3 = schedule.workingCategory3,
                accountId = schedule.account?.id,
                accountName = schedule.account?.name,
                accountExternalKey = schedule.account?.externalKey,
                isClockIn = schedule.commuteLogSfid != null
            )
        }
    }
}

data class DailySummaryDto(
    val date: String,
    val displayExpected: Int,
    val displayActual: Int,
    val promotionExpected: Int,
    val promotionActual: Int,
    val annualLeave: Int,
    val compensatoryLeave: Int
)

data class MonthlyScheduleWithSummaryDto(
    val schedules: List<TeamScheduleDto>,
    val dailySummary: List<DailySummaryDto>
)

data class TeamScheduleCreateResultDto(
    val id: Long
)
