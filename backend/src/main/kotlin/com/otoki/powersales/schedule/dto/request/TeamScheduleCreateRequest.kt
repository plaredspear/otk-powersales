package com.otoki.powersales.schedule.dto.request

import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingCategory2
import com.otoki.powersales.common.enums.WorkingCategory3
import com.otoki.powersales.common.enums.WorkingType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class TeamScheduleCreateRequest(
    @field:NotBlank(message = "사원 ID는 필수입니다")
    val employeeCode: String,

    @field:NotBlank(message = "근무일자는 필수입니다")
    val workingDate: String,

    @field:NotNull(message = "근무형태는 필수입니다")
    val workingType: WorkingType,

    val workingCategory1: WorkingCategory1? = null,
    val workingCategory2: WorkingCategory2? = null,
    val workingCategory3: WorkingCategory3? = null,
    val accountId: Long? = null
)
