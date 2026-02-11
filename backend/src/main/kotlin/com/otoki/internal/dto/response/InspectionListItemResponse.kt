package com.otoki.internal.dto.response

import com.otoki.internal.entity.Inspection

/**
 * 현장 점검 목록 항목 응답 DTO
 */
data class InspectionListItemResponse(
    val id: Long,
    val category: String,
    val storeName: String,
    val storeId: Long,
    val inspectionDate: String,
    val fieldType: String,
    val fieldTypeCode: String
) {
    companion object {
        fun from(inspection: Inspection): InspectionListItemResponse {
            return InspectionListItemResponse(
                id = inspection.id,
                category = inspection.category.name,
                storeName = inspection.storeName,
                storeId = inspection.store.id,
                inspectionDate = inspection.inspectionDate.toString(),
                fieldType = inspection.fieldTypeName,
                fieldTypeCode = inspection.fieldTypeCode
            )
        }
    }
}
