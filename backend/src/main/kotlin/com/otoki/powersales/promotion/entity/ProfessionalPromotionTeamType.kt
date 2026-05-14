package com.otoki.powersales.promotion.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class ProfessionalPromotionTeamType(
    val displayName: String
) {
    RAMEN_SALE("라면세일조"),
    FRESH_SALE_REFRIGERATED("프레시세일조_냉장"),
    FRESH_SALE_FROZEN("프레시세일조_냉동"),
    FRESH_SALE_DUMPLING("프레시세일조_만두"),
    CURRY_PROMOTION("카레행사조");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): ProfessionalPromotionTeamType =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 전문행사조 유형: $value")

        fun fromDisplayNameOrNull(value: String?): ProfessionalPromotionTeamType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
