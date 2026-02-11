package com.otoki.internal.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

/**
 * 현장 점검 등록 요청 DTO
 * multipart/form-data로 전송되며, 사진 파일은 MultipartFile[]로 별도 처리
 */
data class InspectionCreateRequest(
    @field:NotNull(message = "테마 ID는 필수입니다")
    @field:Positive(message = "테마 ID는 양수여야 합니다")
    val themeId: Long?,

    @field:NotBlank(message = "분류는 필수입니다")
    val category: String?,

    @field:NotNull(message = "거래처 ID는 필수입니다")
    @field:Positive(message = "거래처 ID는 양수여야 합니다")
    val storeId: Long?,

    @field:NotBlank(message = "점검일은 필수입니다")
    val inspectionDate: String?,

    @field:NotBlank(message = "현장 유형 코드는 필수입니다")
    val fieldTypeCode: String?,

    // 자사 점검 관련 필드
    @field:Size(max = 500, message = "설명은 최대 500자입니다")
    val description: String? = null,

    val productCode: String? = null,

    // 경쟁사 점검 관련 필드
    @field:Size(max = 100, message = "경쟁사명은 최대 100자입니다")
    val competitorName: String? = null,

    @field:Size(max = 500, message = "경쟁사 활동 내용은 최대 500자입니다")
    val competitorActivity: String? = null,

    val competitorTasting: Boolean? = null,

    @field:Size(max = 100, message = "경쟁사 상품명은 최대 100자입니다")
    val competitorProductName: String? = null,

    @field:Positive(message = "제품 가격은 양수여야 합니다")
    val competitorProductPrice: Int? = null,

    @field:Positive(message = "판매 수량은 양수여야 합니다")
    val competitorSalesQuantity: Int? = null
)
