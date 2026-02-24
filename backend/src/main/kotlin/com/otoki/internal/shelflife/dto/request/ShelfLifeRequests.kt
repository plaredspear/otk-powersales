package com.otoki.internal.shelflife.dto.request

/* --- 전체 주석 처리: V1 Entity 리매핑 (Spec 77) ---
 * ShelfLifeController/Service 비활성화로 Request DTO도 주석 처리.

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class ShelfLifeCreateRequest(
    @field:NotNull(message = "거래처 ID는 필수입니다")
    @field:Positive(message = "거래처 ID는 양수여야 합니다")
    val storeId: Long?,

    @field:NotBlank(message = "제품코드는 필수입니다")
    val productCode: String?,

    @field:NotBlank(message = "유통기한은 필수입니다")
    @field:Pattern(
        regexp = "^\\d{4}-\\d{2}-\\d{2}$",
        message = "유통기한은 YYYY-MM-DD 형식이어야 합니다"
    )
    val expiryDate: String?,

    @field:NotBlank(message = "알림 날짜는 필수입니다")
    @field:Pattern(
        regexp = "^\\d{4}-\\d{2}-\\d{2}$",
        message = "알림 날짜는 YYYY-MM-DD 형식이어야 합니다"
    )
    val alertDate: String?,

    @field:Size(max = 500, message = "설명은 최대 500자까지 입력 가능합니다")
    val description: String? = null
)

data class ShelfLifeUpdateRequest(
    @field:NotBlank(message = "유통기한은 필수입니다")
    @field:Pattern(
        regexp = "^\\d{4}-\\d{2}-\\d{2}$",
        message = "유통기한은 YYYY-MM-DD 형식이어야 합니다"
    )
    val expiryDate: String?,

    @field:NotBlank(message = "알림 날짜는 필수입니다")
    @field:Pattern(
        regexp = "^\\d{4}-\\d{2}-\\d{2}$",
        message = "알림 날짜는 YYYY-MM-DD 형식이어야 합니다"
    )
    val alertDate: String?,

    @field:Size(max = 500, message = "설명은 최대 500자까지 입력 가능합니다")
    val description: String? = null
)

data class ShelfLifeBatchDeleteRequest(
    @field:NotEmpty(message = "삭제할 항목을 선택해주세요")
    val ids: List<@Positive(message = "ID는 양수여야 합니다") Long>?
)

--- */
