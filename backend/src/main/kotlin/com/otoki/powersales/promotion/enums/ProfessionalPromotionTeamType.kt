package com.otoki.powersales.promotion.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * SF `ProfessionalPromotionTeamMaster__c.ProfessionalPromotionTeam__c` picklist 정합 enum.
 *
 * - SF restricted = true (`<sorted>false</sorted>` — 정의된 순서를 그대로 UI 노출).
 * - enum 선언 순서가 SF picklist 정의 순서 (라면세일조 / 프레시세일조_냉동 / 프레시세일조_냉장 /
 *   프레시세일조_만두 / 카레행사조) 와 일치 — UI 드롭다운 순서로 그대로 사용된다.
 */
enum class ProfessionalPromotionTeamType(
    val displayName: String
) {
    RAMEN_SALE("라면세일조"),
    FRESH_SALE_FROZEN("프레시세일조_냉동"),
    FRESH_SALE_REFRIGERATED("프레시세일조_냉장"),
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
