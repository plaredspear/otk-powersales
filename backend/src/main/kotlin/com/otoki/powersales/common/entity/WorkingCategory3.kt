package com.otoki.powersales.common.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `StaffReview__c.DKRetail_WorkingCategory3__c` picklist enum.
 *
 * 단일 권위: docs/plan/old_source_260408/sf-object-meta/_raw/StaffReview__c.json picklistValues
 * Spec #711 §6 — 예외 없이 SF picklist 정의 그대로 enum 변환.
 */
enum class WorkingCategory3(
    val displayName: String
) {
    FIXED("고정"),
    ALTERNATE("격고"),
    PATROL("순회");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): WorkingCategory3 =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 근무카테고리3: $value")

        fun fromDisplayNameOrNull(value: String?): WorkingCategory3? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
