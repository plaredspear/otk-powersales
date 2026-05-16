package com.otoki.powersales.schedule.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/**
 * UC-03 단건 편집 요청.
 * 레거시 SF 표준 레코드 편집 폼과 동등 — 사용자가 폼에서 입력 가능한 모든 필드를 전송.
 * 권한별 필드 화이트리스트는 service 측에서 평가:
 *   - SYSTEM_ADMIN / SALES_SUPPORT 는 confirmed=true 후에도 모든 필드 변경 가능
 *   - LEADER / 그 외는 confirmed=true 후에는 endDate 만 변경 가능 (다른 필드 변경 시 차단)
 */
data class AdminScheduleUpdateRequest(
    @field:NotBlank(message = "사번은 필수입니다")
    val employeeCode: String,

    @field:NotBlank(message = "거래처코드는 필수입니다")
    val accountCode: String,

    @field:NotBlank(message = "근무형태3은 필수입니다")
    val typeOfWork3: String,

    @field:NotBlank(message = "근무형태4는 필수입니다")
    val typeOfWork4: String,

    @field:NotBlank(message = "근무형태5는 필수입니다")
    val typeOfWork5: String,

    @field:NotNull(message = "시작일은 필수입니다")
    val startDate: LocalDate,

    val endDate: LocalDate? = null
)
