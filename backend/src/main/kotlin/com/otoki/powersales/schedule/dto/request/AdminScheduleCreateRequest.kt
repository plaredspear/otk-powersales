package com.otoki.powersales.schedule.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class AdminScheduleCreateRequest(
    @field:NotBlank(message = "사번은 필수입니다")
    val employeeCode: String,

    @field:NotBlank(message = "거래처코드는 필수입니다")
    val accountCode: String,

    @field:NotBlank(message = "근무형태1은 필수입니다")
    val typeOfWork1: String,

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
