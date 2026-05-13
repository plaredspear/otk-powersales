package com.otoki.powersales.notice.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * SF picklist `DKRetail__Scope__c` (2 옵션 — 영업사원/현장여사원) enum.
 * SF 원본 옵션값을 displayName 으로 보존 — README §6.6 v2.2 정책 준수.
 */
enum class NoticeScope(
    val displayName: String
) {
    SALES_EMPLOYEE("영업사원"),
    FIELD_FEMALE_EMPLOYEE("현장여사원");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): NoticeScope =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 NoticeScope: $value")

        fun fromDisplayNameOrNull(value: String?): NoticeScope? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
