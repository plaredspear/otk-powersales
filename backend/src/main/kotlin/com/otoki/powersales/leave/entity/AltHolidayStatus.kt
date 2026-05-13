package com.otoki.powersales.leave.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class AltHolidayStatus(
    val displayName: String
) {
    NEW("신규"),
    APPROVED("승인"),
    ADJUSTED("조정"),
    REJECTED("반려");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): AltHolidayStatus =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 AltHolidayStatus: $value")

        fun fromDisplayNameOrNull(value: String?): AltHolidayStatus? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
