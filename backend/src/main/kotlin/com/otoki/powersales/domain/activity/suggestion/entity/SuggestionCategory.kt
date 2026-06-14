package com.otoki.powersales.domain.activity.suggestion.entity

/**
 * 제안 분류 Enum.
 *
 * SF Picklist `DKRetail__Proposal__c.Category__c` 의 3값과 정합.
 */
enum class SuggestionCategory(val displayName: String) {
    NEW_PRODUCT("신제품 제안"),
    EXISTING_PRODUCT("기존제품 상품가치 향상"),
    LOGISTICS_CLAIM("물류 클레임");

    companion object {
        fun fromDisplayNameOrNull(value: String?): SuggestionCategory? =
            value?.let { v -> entries.firstOrNull { it.displayName == v } }
    }
}
