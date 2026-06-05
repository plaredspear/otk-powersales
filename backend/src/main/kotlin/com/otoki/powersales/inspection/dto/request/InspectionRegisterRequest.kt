package com.otoki.powersales.inspection.dto.request

import com.otoki.powersales.inspection.enums.InspectionCategory
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/**
 * 현장점검 등록 요청 DTO.
 *
 * mobile `POST /api/v1/mobile/inspections` 의 `@RequestPart("request")` JSON. photos 는 별도 multipart part.
 * category 는 OWN/COMPETITOR (InspectionCategory). fieldTypeCode 는 InspectionFieldType code (MAIN_SHELF 등).
 */
data class InspectionRegisterRequest(
    @field:NotNull(message = "themeId는 필수입니다")
    val themeId: Long?,

    @field:NotNull(message = "category는 필수입니다")
    val category: InspectionCategory?,

    @field:NotNull(message = "accountId는 필수입니다")
    val accountId: Long?,

    @field:NotNull(message = "inspectionDate는 필수입니다")
    val inspectionDate: LocalDate?,

    @field:NotNull(message = "fieldTypeCode는 필수입니다")
    val fieldTypeCode: String?,

    val description: String? = null,
    val productCode: String? = null,
    val competitorName: String? = null,
    val competitorActivity: String? = null,
    val competitorTasting: Boolean? = null,
    val competitorProductName: String? = null,
    val competitorProductPrice: Int? = null,
    val competitorSalesQuantity: Int? = null
)
