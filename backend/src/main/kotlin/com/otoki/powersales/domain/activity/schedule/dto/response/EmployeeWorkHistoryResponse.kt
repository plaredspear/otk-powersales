package com.otoki.powersales.domain.activity.schedule.dto.response

import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import java.time.LocalDate

/**
 * 여사원 상세 페이지 — 근무이력 항목 (TeamMemberSchedule 기반).
 *
 * 최근 N개를 working_date desc + created_at desc 로 정렬해 반환.
 */
data class EmployeeWorkHistoryItem(
    val id: Long,
    val workingDate: LocalDate?,
    val workingType: String?,
    val workingCategory1: String?,
    val workingCategory3: String?,
    val workingCategory4: String?,
    val accountName: String?,
    val accountExternalKey: String?,
    val isClockIn: Boolean,
) {
    companion object {
        fun from(schedule: TeamMemberSchedule): EmployeeWorkHistoryItem = EmployeeWorkHistoryItem(
            id = schedule.id,
            workingDate = schedule.workingDate,
            workingType = schedule.workingType?.displayName,
            workingCategory1 = schedule.workingCategory1?.displayName,
            workingCategory3 = schedule.workingCategory3?.displayName,
            workingCategory4 = schedule.workingCategory4,
            accountName = schedule.account?.name,
            accountExternalKey = schedule.account?.externalKey,
            isClockIn = schedule.attendanceLog != null,
        )
    }
}

data class EmployeeWorkHistoryResponse(
    val items: List<EmployeeWorkHistoryItem>,
)
