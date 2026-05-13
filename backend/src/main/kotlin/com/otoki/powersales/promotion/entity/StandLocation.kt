package com.otoki.powersales.promotion.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * SF picklist `DKRetail__StandLocation__c` (6 옵션) enum.
 * SF 원본 옵션값을 displayName 으로 보존 — README §6.6 v2.2 정책 준수.
 */
enum class StandLocation(
    val displayName: String,
    val displayOrder: Int
) {
    FROZEN_EVENT("냉동행사장", 1),
    ISLAND("아일랜드", 2),
    END_CAP("엔드", 3),
    FLAT_TABLE("평대", 4),
    FOOD_TRUCK("푸드트럭", 5),
    EVENT_STAND("행사매대", 6);

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayNameStrict(value: String): StandLocation =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 StandLocation: $value")

        fun fromDisplayName(name: String): StandLocation? =
            entries.find { it.displayName == name }

        fun fromDisplayNameOrNull(value: String?): StandLocation? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
