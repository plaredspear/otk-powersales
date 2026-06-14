package com.otoki.powersales.domain.activity.schedule.dto.response

import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule

/**
 * 조장 대리 일정 등록 응답 DTO (Spec #554 P1-B).
 */
data class LeaderScheduleCreateResponse(
    val scheduleId: Long,
    val targetEmployeeId: Long,
    val workingDate: String,
    val workingType: String,
    val workingCategory3: String,
    val proxyRegisteredBy: Long
) {
    companion object {
        fun from(entity: TeamMemberSchedule): LeaderScheduleCreateResponse =
            LeaderScheduleCreateResponse(
                scheduleId = entity.id,
                targetEmployeeId = entity.employee?.id
                    ?: error("TeamMemberSchedule.employee 가 비어 있습니다"),
                workingDate = entity.workingDate?.toString()
                    ?: error("TeamMemberSchedule.workingDate 가 비어 있습니다"),
                workingType = entity.workingType?.displayName
                    ?: error("TeamMemberSchedule.workingType 이 비어 있습니다"),
                workingCategory3 = entity.workingCategory3?.displayName
                    ?: error("TeamMemberSchedule.workingCategory3 이 비어 있습니다"),
                proxyRegisteredBy = entity.proxyRegisteredBy
                    ?: error("TeamMemberSchedule.proxyRegisteredBy 가 비어 있습니다")
            )
    }
}
