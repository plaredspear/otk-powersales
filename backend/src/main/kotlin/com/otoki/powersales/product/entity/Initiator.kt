package com.otoki.powersales.product.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `NewProduct__c.Initiator__c` picklist enum (발의자).
 *
 * 단일 권위: docs/plan/old_source_260408/sf-object-meta/_raw/NewProduct__c.json picklistValues
 * Spec #737 §3.5 — SF picklist 정의 그대로 enum 변환.
 */
enum class Initiator(
    val displayName: String
) {
    MARKETING_PROPOSAL("마케팅발의"),
    INSTRUCTION("지시");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): Initiator =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 발의자: $value")

        fun fromDisplayNameOrNull(value: String?): Initiator? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
