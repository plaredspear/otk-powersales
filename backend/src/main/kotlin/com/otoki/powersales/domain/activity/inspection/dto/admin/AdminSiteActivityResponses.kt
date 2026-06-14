package com.otoki.powersales.domain.activity.inspection.dto.admin

import com.otoki.powersales.domain.activity.inspection.dto.response.InspectionPhotoResponse
import com.otoki.powersales.domain.activity.inspection.entity.SiteActivity
import com.otoki.powersales.domain.activity.inspection.enums.InspectionCategory
import com.otoki.powersales.domain.activity.inspection.enums.InspectionFieldType

/**
 * admin 현장점검 목록 응답 (페이징 wrapper).
 */
data class AdminSiteActivityListResponse(
    val content: List<AdminSiteActivityListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

/**
 * admin 현장점검 목록 항목. mobile 목록 + 사원명/소속 (admin 관점).
 */
data class AdminSiteActivityListItem(
    val id: Long,
    val category: String,
    val accountName: String,
    val themeName: String,
    val employeeName: String,
    val employeeOrgName: String?,
    val inspectionDate: String,
    val fieldType: String,
    val fieldTypeCode: String,
    val createdAt: String
) {
    companion object {
        fun from(activity: SiteActivity): AdminSiteActivityListItem {
            val fieldType = InspectionFieldType.fromStoredValue(activity.category)
            return AdminSiteActivityListItem(
                id = activity.id,
                category = InspectionCategory.fromStoredValue(activity.productType)?.name ?: "",
                accountName = activity.account?.name ?: "",
                themeName = activity.inspectionTheme?.title ?: "",
                employeeName = activity.employee?.name ?: "",
                employeeOrgName = activity.employee?.orgName,
                inspectionDate = activity.activityDate?.toString() ?: "",
                fieldType = fieldType?.displayName ?: (activity.category ?: ""),
                fieldTypeCode = fieldType?.name ?: "",
                createdAt = activity.createdAt?.toString() ?: ""
            )
        }
    }
}

/**
 * admin 현장점검 상세. 본문 전체 + 경쟁사 활동 + 사진 + 사원/소속.
 */
data class AdminSiteActivityDetailResponse(
    val id: Long,
    val category: String,
    val accountName: String,
    val accountId: Long,
    val themeName: String,
    val themeId: Long,
    val employeeId: Long,
    val employeeName: String,
    val employeeOrgName: String?,
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
        fun from(activity: SiteActivity, photos: List<InspectionPhotoResponse>): AdminSiteActivityDetailResponse {
            val fieldType = InspectionFieldType.fromStoredValue(activity.category)
            return AdminSiteActivityDetailResponse(
                id = activity.id,
                category = InspectionCategory.fromStoredValue(activity.productType)?.name ?: "",
                accountName = activity.account?.name ?: "",
                accountId = activity.account?.id ?: 0,
                themeName = activity.inspectionTheme?.title ?: "",
                themeId = activity.inspectionTheme?.id ?: 0,
                employeeId = activity.employee?.id ?: 0,
                employeeName = activity.employee?.name ?: "",
                employeeOrgName = activity.employee?.orgName,
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

        private fun sampleTastFlagToBoolean(value: String?): Boolean? =
            when (value?.trim()?.uppercase()) {
                "Y" -> true
                "N" -> false
                else -> null
            }
    }
}
