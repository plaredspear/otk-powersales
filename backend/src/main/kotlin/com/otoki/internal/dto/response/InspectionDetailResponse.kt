package com.otoki.internal.dto.response

import com.otoki.internal.entity.Inspection

/**
 * 현장 점검 상세 응답 DTO
 * 분류(자사/경쟁사)에 따라 필드가 달라진다.
 */
data class InspectionDetailResponse(
    val id: Long,
    val category: String,
    val storeName: String,
    val storeId: Long,
    val themeName: String,
    val themeId: Long,
    val inspectionDate: String,
    val fieldType: String,
    val fieldTypeCode: String,

    // 자사 점검 관련 필드
    val description: String?,
    val productCode: String?,
    val productName: String?,

    // 경쟁사 점검 관련 필드
    val competitorName: String?,
    val competitorActivity: String?,
    val competitorTasting: Boolean?,
    val competitorProductName: String?,
    val competitorProductPrice: Int?,
    val competitorSalesQuantity: Int?,

    // Phase2: PhotoResponse 주석 처리됨 - photos 제거
    // val photos: List<PhotoResponse>,
    val createdAt: String
) {
    companion object {
        fun from(inspection: Inspection): InspectionDetailResponse {
            return InspectionDetailResponse(
                id = inspection.id,
                category = inspection.category.name,
                storeName = inspection.storeName,
                storeId = inspection.store.id,
                themeName = inspection.theme.name,
                themeId = inspection.theme.id,
                inspectionDate = inspection.inspectionDate.toString(),
                fieldType = inspection.fieldTypeName,
                fieldTypeCode = inspection.fieldTypeCode,
                description = inspection.description,
                productCode = inspection.productCode,
                productName = inspection.productName,
                competitorName = inspection.competitorName,
                competitorActivity = inspection.competitorActivity,
                competitorTasting = inspection.competitorTasting,
                competitorProductName = inspection.competitorProductName,
                competitorProductPrice = inspection.competitorProductPrice,
                competitorSalesQuantity = inspection.competitorSalesQuantity,
                // Phase2: photos 비활성화
                createdAt = inspection.createdAt.toString()
            )
        }
    }
}
