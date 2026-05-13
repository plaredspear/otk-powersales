package com.otoki.powersales.product.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `DKRetail__Product__c.DKRetail__StoreCondition__c` picklist enum (보관 조건).
 *
 * 단일 권위: Salesforce describe 메타 (`DKRetail__Product__c`) picklistValues — `실온`, `냉장`
 * Spec #754 §3.1 — SF picklist 정의 그대로 enum 변환.
 */
enum class StorageCondition(
    val displayName: String
) {
    ROOM_TEMP("실온"),
    REFRIGERATED("냉장");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): StorageCondition =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 보관 조건: $value")

        fun fromDisplayNameOrNull(value: String?): StorageCondition? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
