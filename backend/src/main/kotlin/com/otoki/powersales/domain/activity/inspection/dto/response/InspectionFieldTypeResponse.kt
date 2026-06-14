package com.otoki.powersales.domain.activity.inspection.dto.response

import com.otoki.powersales.domain.activity.inspection.enums.InspectionFieldType

/**
 * 현장유형 응답 DTO. mobile `GET /api/v1/mobile/inspections/field-types` 계약 (`{code, name}`).
 */
data class InspectionFieldTypeResponse(
    val code: String,
    val name: String
) {
    companion object {
        fun from(type: InspectionFieldType): InspectionFieldTypeResponse =
            InspectionFieldTypeResponse(code = type.name, name = type.displayName)
    }
}
