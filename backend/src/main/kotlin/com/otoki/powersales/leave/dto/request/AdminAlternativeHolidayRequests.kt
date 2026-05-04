package com.otoki.powersales.leave.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class AlternativeHolidayCreateRequest(
    @field:NotBlank(message = "사번은 필수입니다")
    @field:Size(max = 20, message = "사번은 20자 이하여야 합니다")
    val employeeCode: String,

    @field:NotNull(message = "대상일은 필수입니다")
    val actualWorkDate: LocalDate,

    @field:NotNull(message = "신청일은 필수입니다")
    val targetAltHolidayDate: LocalDate
)

data class AlternativeHolidayApproveRequest(
    val confirmAltHolidayDate: LocalDate? = null
)

data class AlternativeHolidayRejectRequest(
    @field:NotBlank(message = "변경 사유는 필수입니다")
    @field:Size(max = 500, message = "변경 사유는 500자 이하여야 합니다")
    val changeReason: String
)
