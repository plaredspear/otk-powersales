package com.otoki.powersales.leave.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class HolidayType(
    val displayName: String
) {
    PUBLIC_HOLIDAY("공휴일"),
    WEEKEND("주말"),
    OTHER("기타");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): HolidayType =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 휴일구분: $value")

        fun fromDisplayNameOrNull(value: String?): HolidayType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
