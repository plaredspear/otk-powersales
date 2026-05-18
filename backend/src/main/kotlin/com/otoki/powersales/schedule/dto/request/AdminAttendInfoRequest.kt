package com.otoki.powersales.schedule.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class AdminAttendInfoSearchRequest(
    val employeeId: Long? = null,
    val employeeCode: String? = null,
    val attendType: String? = null,
    val startDateFrom: LocalDate? = null,
    val startDateTo: LocalDate? = null,
    val status: String? = null,
    val keyword: String? = null,
)

data class AdminAttendInfoCreateRequest(
    @field:NotBlank(message = "사원번호는 필수입니다")
    val employeeCode: String,

    @field:NotBlank(message = "근태유형은 필수입니다")
    val attendType: String,

    @field:NotBlank(message = "시작일은 필수입니다")
    val startDate: String,

    @field:NotBlank(message = "종료일은 필수입니다")
    val endDate: String,

    @field:NotBlank(message = "상태는 필수입니다")
    val status: String,

    @field:NotBlank(message = "사유는 필수입니다")
    @field:Size(min = 5, message = "사유는 최소 5자 이상이어야 합니다")
    val reason: String,
)

data class AdminAttendInfoUpdateRequest(
    val attendType: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val status: String? = null,

    @field:Size(min = 5, message = "사유는 최소 5자 이상이어야 합니다")
    val reason: String? = null,
)
