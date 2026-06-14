package com.otoki.powersales.domain.activity.safetycheck.dto.response

data class SafetyCheckItemsResponse(
    val categories: List<CategoryInfo>
) {
    data class CategoryInfo(
        val questionNum: Int,
        val title: String,
        val inputType: String,
        val required: Boolean,
        val options: List<String>?,
        val items: List<CheckItemInfo>
    )

    data class CheckItemInfo(
        val seqNum: Int,
        val contents: String
    )
}
