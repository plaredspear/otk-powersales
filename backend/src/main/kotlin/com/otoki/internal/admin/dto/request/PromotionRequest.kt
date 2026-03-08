package com.otoki.internal.admin.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class PromotionCreateRequest(
    @field:NotBlank(message = "행사명은 필수입니다")
    @field:Size(max = 200, message = "행사명은 200자 이하여야 합니다")
    val promotionName: String,

    val promotionType: String? = null,

    @field:NotNull(message = "거래처 ID는 필수입니다")
    val accountId: Int,

    @field:NotNull(message = "시작일은 필수입니다")
    val startDate: LocalDate,

    @field:NotNull(message = "종료일은 필수입니다")
    val endDate: LocalDate,

    val primaryProductId: Long? = null,

    @field:Size(max = 500, message = "기타상품은 500자 이하여야 합니다")
    val otherProduct: String? = null,

    @field:Size(max = 1000, message = "메시지는 1000자 이하여야 합니다")
    val message: String? = null,

    @field:Size(max = 200, message = "매대 위치는 200자 이하여야 합니다")
    val standLocation: String? = null,

    val targetAmount: Long? = null
)
