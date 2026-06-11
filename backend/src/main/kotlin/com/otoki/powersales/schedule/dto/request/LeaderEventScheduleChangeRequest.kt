package com.otoki.powersales.schedule.dto.request

import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/**
 * 조장 행사 일정 변경 요청 DTO (레거시 `scheduleChangePromo` updatePromtSchedule M 동등).
 *
 * 행사 변경은 담당 여사원([targetEmployeeId])과 투입일([workingDate])만 재배정한다.
 * 거래처/근무유형은 행사 마스터 파생이라 변경 대상이 아니다. (JSON 키 camelCase)
 */
data class LeaderEventScheduleChangeRequest(
    @field:NotNull(message = "대상 직원 ID는 필수입니다")
    val targetEmployeeId: Long?,

    @field:NotNull(message = "투입일은 필수입니다")
    val workingDate: LocalDate?,
)
