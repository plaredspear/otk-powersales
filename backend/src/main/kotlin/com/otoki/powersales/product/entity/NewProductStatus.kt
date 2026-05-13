package com.otoki.powersales.product.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `NewProduct__c.Status__c` picklist enum (신제품 진행 상태).
 *
 * 단일 권위: Salesforce describe 메타 (`NewProduct__c`) picklistValues
 * Spec #737 §3.5 — SF picklist 정의 그대로 enum 변환.
 */
enum class NewProductStatus(
    val displayName: String
) {
    PLANNED("진행예정"),
    IN_PROGRESS("진행중"),
    COMPLETED("완료");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): NewProductStatus =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 신제품 상태: $value")

        fun fromDisplayNameOrNull(value: String?): NewProductStatus? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
