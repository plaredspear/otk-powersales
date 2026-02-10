package com.otoki.internal.dto.request

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * 주문서 임시저장 / 제출 Request DTO
 */
data class OrderDraftRequest(
    @field:NotNull(message = "거래처 ID는 필수입니다")
    @field:Positive(message = "거래처 ID는 양수여야 합니다")
    val clientId: Long?,

    @field:NotBlank(message = "납기일은 필수입니다")
    val deliveryDate: String?,

    @field:NotEmpty(message = "제품 목록은 1개 이상이어야 합니다")
    @field:Valid
    val items: List<DraftItemRequest>?
)

/**
 * 임시저장 제품 항목 Request DTO
 */
data class DraftItemRequest(
    @field:NotBlank(message = "제품코드는 필수입니다")
    val productCode: String?,

    @field:NotNull(message = "박스 수량은 필수입니다")
    @field:Min(value = 0, message = "박스 수량은 0 이상이어야 합니다")
    val boxQuantity: Int?,

    @field:NotNull(message = "낱개 수량은 필수입니다")
    @field:Min(value = 0, message = "낱개 수량은 0 이상이어야 합니다")
    val pieceQuantity: Int?
)
