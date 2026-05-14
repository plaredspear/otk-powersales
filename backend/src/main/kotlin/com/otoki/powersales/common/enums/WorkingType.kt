package com.otoki.powersales.common.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * SF picklist `DKRetail__WorkingType__c` (2 옵션 — 근무/연차) enum.
 * SF 원본 옵션값을 displayName 으로 보존 — README §6.6 v2.2 정책 준수.
 */
enum class WorkingType(
    val displayName: String
) {
    WORK("근무"),
    ANNUAL_LEAVE("연차"),
    // SF picklist 옵션 외 — 레거시 운영 데이터(대휴 일정) 호환을 위해 enum 에 포함. SF org picklist 정비 필요.
    ALT_HOLIDAY("대휴");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): WorkingType =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 근무유형: $value")

        fun fromDisplayNameOrNull(value: String?): WorkingType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
