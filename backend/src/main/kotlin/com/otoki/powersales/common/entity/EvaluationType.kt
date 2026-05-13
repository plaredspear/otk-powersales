package com.otoki.powersales.common.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `HQReview__c.EvaluationyType__c` (평가유형) picklist enum.
 *
 * 단일 권위: docs/plan/old_source_260408/sf-object-meta/_raw/HQReview__c.json picklistValues
 * Spec #708 §6 — 예외 없이 SF picklist 정의 그대로 enum 변환.
 */
enum class EvaluationType(
    val displayName: String
) {
    FIRST_DIVISION("제1사업부"),
    DISTRIBUTION_HQ("유통총괄실");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): EvaluationType =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 평가유형: $value")

        fun fromDisplayNameOrNull(value: String?): EvaluationType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
