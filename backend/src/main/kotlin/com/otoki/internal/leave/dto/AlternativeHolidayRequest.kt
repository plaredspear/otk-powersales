package com.otoki.internal.leave.dto

import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class AlternativeHolidayRequest(
    @field:NotNull(message = "대상일은 필수입니다")
    val actualWorkDate: LocalDate,

    @field:NotNull(message = "신청일은 필수입니다")
    val targetAltHolidayDate: LocalDate
)
