package com.otoki.powersales.domain.org.leave.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class HolidayMasterCreateRequest(
    @field:NotNull(message = "공휴일 날짜는 필수입니다")
    val holidayDate: LocalDate,

    @field:NotBlank(message = "공휴일명은 필수입니다")
    @field:Size(max = 50, message = "공휴일명은 50자 이하여야 합니다")
    val name: String,

    @field:NotBlank(message = "공휴일 유형은 필수입니다")
    val type: String
)

data class HolidayMasterUpdateRequest(
    @field:NotNull(message = "공휴일 날짜는 필수입니다")
    val holidayDate: LocalDate,

    @field:NotBlank(message = "공휴일명은 필수입니다")
    @field:Size(max = 50, message = "공휴일명은 50자 이하여야 합니다")
    val name: String,

    @field:NotBlank(message = "공휴일 유형은 필수입니다")
    val type: String
)
