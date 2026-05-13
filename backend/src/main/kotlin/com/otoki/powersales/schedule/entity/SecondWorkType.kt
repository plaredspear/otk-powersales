package com.otoki.powersales.schedule.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class SecondWorkType(
    val displayName: String
) {
    ROOM_TEMP("상온"),
    FROZEN_REFRIGERATED("냉동/냉장");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): SecondWorkType =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 SecondWorkType: $value")

        fun fromDisplayNameOrNull(value: String?): SecondWorkType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
