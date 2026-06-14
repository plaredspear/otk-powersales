package com.otoki.powersales.platform.common.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `StaffReview__c.DKRetail_WorkingCategory1__c` picklist enum.
 *
 * 단일 권위: Salesforce describe 메타 (`StaffReview__c`) picklistValues
 * Spec #711 §6 — 예외 없이 SF picklist 정의 그대로 enum 변환.
 */
enum class WorkingCategory1(
    val displayName: String
) {
    DISPLAY("진열"),
    EVENT("행사");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): WorkingCategory1 =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 근무카테고리1: $value")

        fun fromDisplayNameOrNull(value: String?): WorkingCategory1? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
