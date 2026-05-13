package com.otoki.powersales.promotion.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * SF picklist `DKRetail__ProductType__c` (3 옵션 — 상온/라면/냉장·냉동) enum.
 * SF 원본 옵션값을 displayName 으로 보존 — README §6.6 v2.2 정책 준수.
 */
enum class ProductTemperatureType(
    val displayName: String
) {
    AMBIENT("상온"),
    NOODLE("라면"),
    COLD("냉장/냉동");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): ProductTemperatureType =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 ProductTemperatureType: $value")

        fun fromDisplayNameOrNull(value: String?): ProductTemperatureType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
