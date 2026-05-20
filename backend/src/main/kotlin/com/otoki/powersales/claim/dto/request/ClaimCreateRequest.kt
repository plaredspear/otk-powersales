package com.otoki.powersales.claim.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal

/**
 * 클레임 등록 요청 DTO (multipart/form-data).
 *
 * claimType1/claimType2 는 SF picklist value (A/B/C, AA/AB.../CF) 를 그대로 받는다.
 * mobile 도 동일 enum value 송신으로 정합.
 */
data class ClaimCreateRequest(
    @field:NotNull(message = "거래처 ID는 필수입니다")
    @field:Positive(message = "거래처 ID는 양수여야 합니다")
    val accountId: Long?,

    @field:NotBlank(message = "제품 코드는 필수입니다")
    val productCode: String?,

    @field:NotBlank(message = "기한 종류는 필수입니다")
    val dateType: String?,

    @field:NotBlank(message = "기한 날짜는 필수입니다")
    val date: String?,

    @field:NotBlank(message = "클레임 대분류는 필수입니다")
    val claimType1: String?,

    @field:NotBlank(message = "클레임 소분류는 필수입니다")
    val claimType2: String?,

    @field:NotBlank(message = "불량 내역은 필수입니다")
    @field:Size(max = 4000, message = "불량 내역은 최대 4000자입니다")
    val defectDescription: String?,

    @field:NotNull(message = "불량 수량은 필수입니다")
    @field:Positive(message = "불량 수량은 양수여야 합니다")
    val defectQuantity: BigDecimal?,

    @field:Positive(message = "구매 금액은 양수여야 합니다")
    val purchaseAmount: BigDecimal? = null,

    /** SF PurchaseMethod value (A/B/C) */
    val purchaseMethodCode: String? = null,

    /**
     * SF RequestType (multipicklist) - 다중 선택은 ";" 구분 문자열로 전달.
     * 레거시 Validation Rule 정합: 최대 4개. service-layer 에서 검증.
     */
    val requestTypeCode: String? = null
)
