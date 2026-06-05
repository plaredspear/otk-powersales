package com.otoki.powersales.inspection.dto.response

import com.otoki.powersales.common.entity.UploadFile
import com.otoki.powersales.inspection.entity.SiteActivity
import com.otoki.powersales.inspection.enums.InspectionCategory
import com.otoki.powersales.inspection.enums.InspectionFieldType

/**
 * 현장점검 상세 응답 DTO.
 *
 * mobile `GET /api/v1/mobile/inspections/{id}` 계약. SF picklist 의 sample_tast_flag(Y/N) 는
 * competitorTasting(Boolean), competitor_proudct_price(BigDecimal 오타 컬럼) 는 competitorProductPrice(Int) 로 변환.
 */
data class InspectionDetailResponse(
    val id: Long,
    val category: String,
    val accountName: String,
    val accountId: Long,
    val themeName: String,
    val themeId: Long,
    val inspectionDate: String,
    val fieldType: String,
    val fieldTypeCode: String,
    val description: String?,
    val productCode: String?,
    val productName: String?,
    val competitorName: String?,
    val competitorActivity: String?,
    val competitorTasting: Boolean?,
    val competitorProductName: String?,
    val competitorProductPrice: Int?,
    val competitorSalesQuantity: Int?,
    val photos: List<InspectionPhotoResponse>,
    val createdAt: String
) {
    companion object {
        fun from(activity: SiteActivity, photos: List<InspectionPhotoResponse>): InspectionDetailResponse {
            val fieldType = InspectionFieldType.fromStoredValue(activity.category)
            return InspectionDetailResponse(
                id = activity.id,
                category = InspectionCategory.fromStoredValue(activity.productType)?.name ?: "",
                accountName = activity.account?.name ?: "",
                accountId = activity.account?.id ?: 0,
                themeName = activity.inspectionTheme?.title ?: "",
                themeId = activity.inspectionTheme?.id ?: 0,
                inspectionDate = activity.activityDate?.toString() ?: "",
                fieldType = fieldType?.displayName ?: (activity.category ?: ""),
                fieldTypeCode = fieldType?.name ?: "",
                description = activity.description,
                productCode = activity.product?.productCode,
                productName = activity.product?.name,
                competitorName = activity.competitorName,
                competitorActivity = activity.competitorActivityDescription,
                competitorTasting = sampleTastFlagToBoolean(activity.sampleTastFlag),
                competitorProductName = activity.competitorProductName,
                competitorProductPrice = activity.competitorProudctPrice?.toInt(),
                competitorSalesQuantity = activity.salesQuantity?.toInt(),
                photos = photos,
                createdAt = activity.createdAt?.toString() ?: ""
            )
        }

        /** SF SampleTastFlag picklist (Y/N) → Boolean. null/blank → null. */
        private fun sampleTastFlagToBoolean(value: String?): Boolean? =
            when (value?.trim()?.uppercase()) {
                "Y" -> true
                "N" -> false
                else -> null
            }
    }
}

/**
 * 현장점검 첨부 사진 응답 DTO. mobile `photos[{id, url}]` 계약.
 */
data class InspectionPhotoResponse(
    val id: Long,
    val url: String
) {
    companion object {
        fun of(uploadFile: UploadFile, url: String): InspectionPhotoResponse =
            InspectionPhotoResponse(id = uploadFile.id, url = url)
    }
}
