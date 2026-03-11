package com.otoki.internal.admin.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import jakarta.validation.constraints.Min
import java.time.LocalDate

data class PromotionEmployeeRequest(
    @field:NotBlank(message = "여사원 SF ID는 필수입니다")
    @field:Size(max = 18, message = "여사원 SF ID는 최대 18자입니다")
    val employeeSfid: String,

    @field:NotNull(message = "투입일은 필수입니다")
    val scheduleDate: LocalDate,

    @field:NotBlank(message = "근무상태는 필수입니다")
    val workStatus: String,

    @field:NotBlank(message = "근무유형1은 필수입니다")
    @field:Size(max = 100, message = "근무유형1은 최대 100자입니다")
    val workType1: String,

    @field:NotBlank(message = "근무유형3은 필수입니다")
    val workType3: String,

    @field:Size(max = 100, message = "근무유형4는 최대 100자입니다")
    val workType4: String? = null,

    @field:Size(max = 100, message = "전문행사조는 최대 100자입니다")
    val professionalPromotionTeam: String? = null,

    @field:Min(value = 0, message = "판매단가는 0 이상이어야 합니다")
    val basePrice: Long? = null,

    @field:Min(value = 0, message = "일일 목표수량은 0 이상이어야 합니다")
    val dailyTargetCount: Int? = null,

    @field:Min(value = 0, message = "목표금액은 0 이상이어야 합니다")
    val targetAmount: Long? = 0,

    @field:Min(value = 0, message = "실적금액은 0 이상이어야 합니다")
    val actualAmount: Long? = 0
)
