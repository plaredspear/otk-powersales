package com.otoki.powersales.domain.activity.schedule.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * AccountViewAll 대리출근 등록 요청 DTO.
 *
 * 조장 대리출근([LeaderProxyAttendanceRequest])과 달리 [branchCode](선택 지점)를 필수로 받아,
 * 대상 여사원이 그 지점 소속인지 서버에서 재검증한다. 진열 거래처는 [displayWorkScheduleId]
 * (진열 마스터 ID), 행사·기배정 거래처는 [scheduleId](team_member_schedule ID) 중 하나 전달.
 * (Jackson SNAKE_CASE — JSON 키 snake_case)
 */
data class ProxyAttendanceRegisterRequest(
    @field:NotBlank(message = "지점을 선택해야 합니다")
    val branchCode: String?,

    @field:NotNull(message = "대상 직원 ID는 필수입니다")
    val targetEmployeeId: Long?,

    val scheduleId: Long? = null,

    val displayWorkScheduleId: Long? = null,
)
