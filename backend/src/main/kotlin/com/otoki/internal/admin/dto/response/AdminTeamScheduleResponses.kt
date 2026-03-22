package com.otoki.internal.admin.dto.response

import com.otoki.internal.sap.entity.Employee
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.schedule.entity.TeamMemberSchedule
import java.time.LocalDate

data class TeamMemberDto(
    val employeeCode: String,
    val name: String
) {
    companion object {
        fun from(employee: Employee): TeamMemberDto = TeamMemberDto(
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
        fun from(
            schedule: TeamMemberSchedule,
            employeeMap: Map<Long, Employee>,
            accountMap: Map<Int, Account>
        ): TeamScheduleDto {
            val employee = schedule.employeeId?.let { employeeMap[it] }
            val account = schedule.accountId?.let { accountMap[it] }
            return TeamScheduleDto(
                id = schedule.id,
                employeeCode = employee?.employeeCode ?: "",
                employeeName = employee?.name ?: "",
                workingDate = schedule.workingDate?.toString() ?: "",
                workingType = schedule.workingType ?: "",
                workingCategory1 = schedule.workingCategory1,
                workingCategory2 = schedule.workingCategory2,
                workingCategory3 = schedule.workingCategory3,
                accountId = schedule.accountId,
                accountName = account?.name,
                accountExternalKey = account?.externalKey,
                isClockIn = schedule.commuteLogId != null
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

data class TeamScheduleCreateResultDto(
    val id: Long
)
