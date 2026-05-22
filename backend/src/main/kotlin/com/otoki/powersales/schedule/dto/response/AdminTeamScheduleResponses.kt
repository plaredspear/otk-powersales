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
    val isClockIn: Boolean,
    val promotionId: Long?
) {
    companion object {
        fun from(schedule: TeamMemberSchedule): TeamScheduleDto {
            return TeamScheduleDto(
                id = schedule.id,
                employeeCode = schedule.employee?.employeeCode ?: "",
                employeeName = schedule.employee?.name ?: "",
                workingDate = schedule.workingDate?.toString() ?: "",
                workingType = schedule.workingType?.displayName ?: "",
                workingCategory1 = schedule.workingCategory1?.displayName,
                workingCategory2 = schedule.workingCategory2?.displayName,
                workingCategory3 = schedule.workingCategory3?.displayName,
                accountId = schedule.account?.id,
                accountName = schedule.account?.name,
                accountExternalKey = schedule.account?.externalKey,
                isClockIn = schedule.attendanceLog != null,
                promotionId = schedule.promotionEmployee?.promotionId
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

/**
 * 여사원 일정 다건 삭제 응답 (Spec #691 P1-B, Q5 옵션 1 — 전체 rollback 정책).
 * 1건이라도 가드 fail 시 도메인 예외 throw → @Transactional 전체 rollback → 본 DTO 미반환.
 */
data class TeamScheduleMassDeleteResponse(
    val deletedCount: Int
)
