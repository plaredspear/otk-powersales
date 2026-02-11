package com.otoki.internal.dto.response

/**
 * 현장 점검 목록 응답 DTO
 */
data class InspectionListResponse(
    val totalCount: Int,
    val items: List<InspectionListItemResponse>
) {
    companion object {
        fun of(items: List<InspectionListItemResponse>): InspectionListResponse {
            return InspectionListResponse(
                totalCount = items.size,
                items = items
            )
        }
    }
}
