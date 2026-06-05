package com.otoki.powersales.schedule.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * 조장 대리 일정 등록 요청 DTO (Spec #554 P1-B).
 *
 * Jackson SNAKE_CASE 설정으로 JSON 키는 snake_case (target_employee_id, working_date 등) 로 노출된다.
 */
data class LeaderScheduleCreateRequest(
    @field:NotNull(message = "대상 직원 ID는 필수입니다")
    val targetEmployeeId: Long?,

    @field:NotBlank(message = "근무일자는 필수입니다")
    val workingDate: String,

    @field:NotBlank(message = "근무 유형은 필수입니다")
    val workingType: String,

    @field:NotBlank(message = "근무 분류 2는 필수입니다")
    val workingCategory2: String,

    @field:NotBlank(message = "근무 분류 3은 필수입니다")
    val workingCategory3: String,

    val accountId: Long? = null,

    val workingCategory1: String? = null
)
