package com.otoki.internal.dto.response

import com.otoki.internal.entity.InspectionFieldType

/**
 * 현장 유형 응답 DTO
 */
data class FieldTypeResponse(
    val code: String,
    val name: String
) {
    companion object {
        fun from(fieldType: InspectionFieldType): FieldTypeResponse {
            return FieldTypeResponse(
                code = fieldType.code,
                name = fieldType.name
            )
        }
    }
}
