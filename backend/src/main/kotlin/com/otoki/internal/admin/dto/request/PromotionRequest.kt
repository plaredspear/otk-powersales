package com.otoki.internal.admin.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class PromotionCreateRequest(
    @field:NotBlank(message = "행사명은 필수입니다")
    @field:Size(max = 200, message = "행사명은 200자 이하여야 합니다")
    val promotionName: String,

    val promotionTypeId: Long? = null,

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

    val targetAmount: Long? = null,

    @field:Size(max = 50, message = "카테고리는 50자 이하여야 합니다")
    val category: String? = null,

    @field:Size(max = 50, message = "제품유형은 50자 이하여야 합니다")
    val productType: String? = null,

    @field:Size(max = 100, message = "지점명은 100자 이하여야 합니다")
    val branchName: String? = null,

    @field:Size(max = 100, message = "전문행사조는 100자 이하여야 합니다")
    val professionalTeam: String? = null,

    @field:Size(max = 50, message = "외부 연동 ID는 50자 이하여야 합니다")
    val externalId: String? = null
)
