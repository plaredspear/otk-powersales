/*
package com.otoki.powersales.inspection.dto.response

import com.otoki.powersales.inspection.entity.Inspection

/ **
 * 현장 점검 목록 항목 응답 DTO
 * /
data class InspectionListItemResponse(
    val id: Long,
    val category: String,
    val accountName: String,
    val accountId: Long,
    val inspectionDate: String,
    val fieldType: String,
    val fieldTypeCode: String
) {
    companion object {
        fun from(inspection: Inspection): InspectionListItemResponse {
            return InspectionListItemResponse(
                id = inspection.id,
                category = inspection.category.name,
                accountName = inspection.accountName,
                accountId = inspection.account.id,
                inspectionDate = inspection.inspectionDate.toString(),
                fieldType = inspection.fieldTypeName,
                fieldTypeCode = inspection.fieldTypeCode
            )
        }
    }
}
*/
