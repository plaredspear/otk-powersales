package com.otoki.powersales.schedule.dto.request

import jakarta.validation.constraints.NotNull

/**
 * 조장 대리출근 등록 요청 DTO (레거시 mngDaily `addScheduleProc` 동등).
 *
 * 진열 거래처는 [displayWorkScheduleId](진열 마스터 ID), 행사·기배정 거래처는 [scheduleId]
 * (team_member_schedule ID) 중 하나를 전달한다. (Jackson SNAKE_CASE — JSON 키 snake_case)
 */
data class LeaderProxyAttendanceRequest(
    @field:NotNull(message = "대상 직원 ID는 필수입니다")
    val targetEmployeeId: Long?,

    val scheduleId: Long? = null,

    val displayWorkScheduleId: Long? = null,
)
