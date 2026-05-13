package com.otoki.powersales.schedule.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * SF picklist `TypeOfWork5__c` (2 옵션 — 상시/임시) enum.
 * SF 원본 옵션값을 displayName 으로 보존 — README §6.6 v2.2 정책 준수.
 */
enum class TypeOfWork5(
    val displayName: String
) {
    REGULAR("상시"),
    TEMPORARY("임시");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): TypeOfWork5 =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 근무형태5: $value")

        fun fromDisplayNameOrNull(value: String?): TypeOfWork5? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
