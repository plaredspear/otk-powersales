package com.otoki.internal.dto.response

data class SafetyCheckItemsResponse(
    val categories: List<CategoryInfo>
) {
    data class CategoryInfo(
        val id: Long,
        val name: String,
        val description: String?,
        val items: List<CheckItemInfo>
    )

    data class CheckItemInfo(
        val id: Long,
        val label: String,
        val sortOrder: Int,
        val required: Boolean
    )
}
