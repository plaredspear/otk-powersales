package com.otoki.powersales.claim.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `DKRetail__Claim__c.DKRetail__Channel__c` (접수채널) picklist enum.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/클레임(DKRetail__Claim__c).md (선택 목록 2개)
 *
 * Spec #606 Q1 옵션 1 결정:
 *   - enum name 은 SF API 이름과 동일 (CRM, CAP) — DB 저장값과도 동일
 *   - JPA 매핑은 `ClaimChannelConverter` 경유 (스펙 §6.3 / AC6 명세 준수)
 */
enum class ClaimChannel(
    val displayName: String
) {
    CRM("CRM"),
    CAP("CAP CRM(CAP)");

    @JsonValue
    fun toJson(): String = name

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromCode(value: String): ClaimChannel =
            entries.find { it.name == value }
                ?: throw IllegalArgumentException("유효하지 않은 접수채널: $value")

        fun fromCodeOrNull(value: String?): ClaimChannel? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name == value }
        }
    }
}
