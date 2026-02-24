package com.otoki.internal.inspection.dto.response

import com.otoki.internal.inspection.entity.InspectionTheme

/**
 * 현장 점검 테마 응답 DTO
 */
data class ThemeResponse(
    val id: Long,
    val name: String,
    val startDate: String,
    val endDate: String
) {
    companion object {
        fun from(theme: InspectionTheme): ThemeResponse {
            return ThemeResponse(
                id = theme.id,
                name = theme.name ?: "",
                startDate = theme.startDate?.toString() ?: "",
                endDate = theme.endDate?.toString() ?: ""
            )
        }
    }
}
