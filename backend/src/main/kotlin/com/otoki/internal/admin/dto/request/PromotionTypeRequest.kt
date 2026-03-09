package com.otoki.internal.admin.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class PromotionTypeRequest(
    @field:NotBlank(message = "행사유형명은 필수입니다")
    @field:Size(max = 50, message = "행사유형명은 50자 이하여야 합니다")
    val name: String,

    @field:NotNull(message = "표시순서는 필수입니다")
    @field:Min(value = 1, message = "표시순서는 1 이상이어야 합니다")
    val displayOrder: Int
)
