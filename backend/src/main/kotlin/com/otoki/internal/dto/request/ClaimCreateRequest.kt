package com.otoki.internal.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

/**
 * 클레임 등록 요청 DTO
 * multipart/form-data로 전송되며, 사진 파일은 MultipartFile로 별도 처리
 */
data class ClaimCreateRequest(
    @field:NotNull(message = "거래처 ID는 필수입니다")
    @field:Positive(message = "거래처 ID는 양수여야 합니다")
    val storeId: Long?,

    @field:NotBlank(message = "제품 코드는 필수입니다")
    val productCode: String?,

    @field:NotBlank(message = "기한 종류는 필수입니다")
    val dateType: String?,

    @field:NotBlank(message = "기한 날짜는 필수입니다")
    val date: String?,

    @field:NotNull(message = "클레임 종류1 ID는 필수입니다")
    @field:Positive(message = "클레임 종류1 ID는 양수여야 합니다")
    val categoryId: Long?,

    @field:NotNull(message = "클레임 종류2 ID는 필수입니다")
    @field:Positive(message = "클레임 종류2 ID는 양수여야 합니다")
    val subcategoryId: Long?,

    @field:NotBlank(message = "불량 내역은 필수입니다")
    @field:Size(max = 1000, message = "불량 내역은 최대 1000자입니다")
    val defectDescription: String?,

    @field:NotNull(message = "불량 수량은 필수입니다")
    @field:Positive(message = "불량 수량은 양수여야 합니다")
    val defectQuantity: Int?,

    // 구매 정보 (선택)
    @field:Positive(message = "구매 금액은 양수여야 합니다")
    val purchaseAmount: Int? = null,

    val purchaseMethodCode: String? = null,

    // 요청사항 (선택)
    val requestTypeCode: String? = null
)
