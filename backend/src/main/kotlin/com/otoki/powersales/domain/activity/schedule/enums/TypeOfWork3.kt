package com.otoki.powersales.domain.activity.schedule.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * SF picklist `TypeOfWork3__c` (3 옵션 — 고정/격고/순회) enum.
 * SF 원본 옵션값을 displayName 으로 보존 — README §6.6 v2.2 정책 준수.
 */
enum class TypeOfWork3(
    val displayName: String
) {
    FIXED("고정"),
    GAP("격고"),
    ROTATION("순회");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): TypeOfWork3 =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 근무형태3: $value")

        fun fromDisplayNameOrNull(value: String?): TypeOfWork3? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
