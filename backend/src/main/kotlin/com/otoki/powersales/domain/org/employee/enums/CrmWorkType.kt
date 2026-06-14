package com.otoki.powersales.domain.org.employee.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `DKRetail__Employee__c.DKRetail__CRM_WorkType__c` picklist enum.
 *
 * SF picklist 원본값 보존. DB 저장 및 JSON 직렬화는 [displayName] (SF 원본값).
 * JPA 매핑은 `CrmWorkTypeConverter` 경유.
 */
enum class CrmWorkType(val displayName: String) {
    HYPHEN("-");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): CrmWorkType =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 CrmWorkType: $value")

        fun fromDisplayNameOrNull(value: String?): CrmWorkType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
