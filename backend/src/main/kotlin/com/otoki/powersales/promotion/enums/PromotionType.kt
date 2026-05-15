package com.otoki.powersales.promotion.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * SF picklist `DKRetail__PromotionType__c` (3 옵션 — 시식 / 권장 / 모음전) enum.
 * SF 원본 옵션값을 displayName 으로 보존 — README §6.6 v2.2 정책 준수.
 */
enum class PromotionType(
    val displayName: String,
    val displayOrder: Int
) {
    SAMPLING("시식", 1),
    RECOMMENDATION("권장", 2),
    COLLECTION("모음전", 3);

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayNameStrict(value: String): PromotionType =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 PromotionType: $value")

        fun fromDisplayNameOrNull(value: String?): PromotionType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
