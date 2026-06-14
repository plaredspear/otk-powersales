package com.otoki.powersales.domain.activity.schedule.dto.response

import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule

/**
 * 조장 행사 일정 변경 응답 DTO (레거시 `scheduleChangePromo` 변경 결과).
 *
 * 재파생된 TeamMemberSchedule 기준 — 거래처/근무유형은 행사 마스터 파생이라 응답에 담지 않는다.
 */
data class LeaderEventScheduleChangeResponse(
    val scheduleId: Long,
    val targetEmployeeId: Long?,
    val workingDate: String?,
) {
    companion object {
        fun from(entity: TeamMemberSchedule): LeaderEventScheduleChangeResponse =
            LeaderEventScheduleChangeResponse(
                scheduleId = entity.id,
                targetEmployeeId = entity.employee?.id,
                workingDate = entity.workingDate?.toString(),
            )
    }
}
