package com.otoki.powersales.schedule.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/**
 * 조장 진열 일정(마스터) 신규 등록 요청 (레거시 `scheduleChange` 진열 추가 동등).
 *
 * 진열 조회는 기간 마스터(`DisplayWorkSchedule`) 기반이라, 추가는 마스터 1행(여사원×거래처×기간)을 생성한다.
 * cat1=진열 고정. (JSON 키 camelCase)
 */
data class LeaderDisplayScheduleCreateRequest(
    @field:NotNull(message = "대상 직원 ID는 필수입니다")
    val targetEmployeeId: Long?,

    @field:NotNull(message = "거래처 ID는 필수입니다")
    val accountId: Long?,

    @field:NotNull(message = "시작일은 필수입니다")
    val startDate: LocalDate?,

    val endDate: LocalDate? = null,

    @field:NotBlank(message = "근무유형(고정/격고/순회)은 필수입니다")
    val typeOfWork3: String?,

    @field:NotBlank(message = "근무유형(상온/냉동냉장)은 필수입니다")
    val typeOfWork4: String?,

    @field:NotBlank(message = "근무유형(상시/임시)은 필수입니다")
    val typeOfWork5: String?,
)
