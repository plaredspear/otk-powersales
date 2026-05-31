package com.otoki.powersales.schedule.dto.response

import com.otoki.powersales.schedule.entity.TeamMemberSchedule

/**
 * 조장 진열 일정 수정 응답 DTO (P7).
 */
data class LeaderScheduleUpdateResponse(
    val scheduleId: Long,
    val targetEmployeeId: Long,
    val workingDate: String,
    val accountId: Int?,
    val accountName: String?,
    val workingCategory3: String?,
) {
    companion object {
        fun from(entity: TeamMemberSchedule): LeaderScheduleUpdateResponse =
            LeaderScheduleUpdateResponse(
                scheduleId = entity.id,
                targetEmployeeId = entity.employee?.id
                    ?: error("TeamMemberSchedule.employee 가 비어 있습니다"),
                workingDate = entity.workingDate?.toString()
                    ?: error("TeamMemberSchedule.workingDate 가 비어 있습니다"),
                accountId = entity.account?.id,
                accountName = entity.account?.name,
                workingCategory3 = entity.workingCategory3?.displayName,
            )
    }
}
