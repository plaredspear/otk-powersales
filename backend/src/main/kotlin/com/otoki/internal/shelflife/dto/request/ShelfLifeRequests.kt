package com.otoki.internal.shelflife.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class ShelfLifeCreateRequest(
    @field:NotBlank(message = "거래처 코드는 필수입니다")
    val accountCode: String,

    @field:NotBlank(message = "거래처명은 필수입니다")
    val accountName: String,

    @field:NotBlank(message = "제품 코드는 필수입니다")
    val productCode: String,

    @field:NotBlank(message = "제품명은 필수입니다")
    val productName: String,

    @field:NotBlank(message = "유통기한은 필수입니다")
    @field:Pattern(
        regexp = "^\\d{4}-\\d{2}-\\d{2}$",
        message = "유통기한은 YYYY-MM-DD 형식이어야 합니다"
    )
    val expirationDate: String,

    @field:NotBlank(message = "알림일은 필수입니다")
    @field:Pattern(
        regexp = "^\\d{4}-\\d{2}-\\d{2}$",
        message = "알림일은 YYYY-MM-DD 형식이어야 합니다"
    )
    val alarmDate: String,

    @field:Size(max = 500, message = "설명은 최대 500자까지 입력 가능합니다")
    val description: String? = null
)

data class ShelfLifeUpdateRequest(
    @field:NotBlank(message = "유통기한은 필수입니다")
    @field:Pattern(
        regexp = "^\\d{4}-\\d{2}-\\d{2}$",
        message = "유통기한은 YYYY-MM-DD 형식이어야 합니다"
    )
    val expirationDate: String,

    @field:NotBlank(message = "알림일은 필수입니다")
    @field:Pattern(
        regexp = "^\\d{4}-\\d{2}-\\d{2}$",
        message = "알림일은 YYYY-MM-DD 형식이어야 합니다"
    )
    val alarmDate: String,

    @field:Size(max = 500, message = "설명은 최대 500자까지 입력 가능합니다")
    val description: String? = null
)

data class ShelfLifeBatchDeleteRequest(
    @field:NotEmpty(message = "삭제할 항목을 선택해주세요")
    val ids: List<Int>
)
