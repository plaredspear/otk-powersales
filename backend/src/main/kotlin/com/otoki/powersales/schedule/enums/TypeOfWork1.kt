package com.otoki.powersales.schedule.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class TypeOfWork1(
    val displayName: String
) {
    DISPLAY("진열");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): TypeOfWork1 =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 근무형태1: $value")

        fun fromDisplayNameOrNull(value: String?): TypeOfWork1? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
