package com.otoki.powersales.domain.activity.inspection.dto.response

import com.otoki.powersales.domain.activity.inspection.entity.SiteActivity
import com.otoki.powersales.domain.activity.inspection.enums.InspectionCategory
import com.otoki.powersales.domain.activity.inspection.enums.InspectionFieldType

/**
 * 현장점검 목록 항목 응답 DTO.
 *
 * mobile `GET /api/v1/mobile/inspections` 계약. category 는 OWN/COMPETITOR, fieldType/fieldTypeCode 는
 * SF Category picklist (본매대 등) 의 한글명/code.
 */
data class InspectionListItem(
    val id: Long,
    val category: String,
    val accountName: String,
    val accountId: Long,
    val inspectionDate: String,
    val fieldType: String,
    val fieldTypeCode: String
) {
    companion object {
        fun from(activity: SiteActivity): InspectionListItem {
            val fieldType = InspectionFieldType.fromStoredValue(activity.category)
            return InspectionListItem(
                id = activity.id,
                category = InspectionCategory.fromStoredValue(activity.productType)?.name ?: "",
                accountName = activity.account?.name ?: "",
                accountId = activity.account?.id ?: 0,
                inspectionDate = activity.activityDate?.toString() ?: "",
                fieldType = fieldType?.displayName ?: (activity.category ?: ""),
                fieldTypeCode = fieldType?.name ?: ""
            )
        }
    }
}
