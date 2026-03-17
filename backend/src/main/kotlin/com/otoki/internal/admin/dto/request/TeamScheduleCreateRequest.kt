package com.otoki.internal.admin.dto.request

import jakarta.validation.constraints.NotBlank

data class TeamScheduleCreateRequest(
    @field:NotBlank(message = "사원 ID는 필수입니다")
    val employeeId: String,

    @field:NotBlank(message = "근무일자는 필수입니다")
    val workingDate: String,

    @field:NotBlank(message = "근무형태는 필수입니다")
    val workingType: String,

    val workingCategory1: String? = null,
    val workingCategory2: String? = null,
    val workingCategory3: String? = null,
    val accountSfid: String? = null
)
