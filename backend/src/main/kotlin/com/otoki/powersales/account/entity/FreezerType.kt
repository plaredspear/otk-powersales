package com.otoki.powersales.account.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `Account.FreezerType__c` (냉장고종류) picklist enum.
 *
 * 단일 권위: Salesforce Object (`Account`) (선택 목록 2개)
 *
 * Spec #602 Q5/Q5-1 결정:
 *   - enum name 은 영문 (컨벤션 보존)
 *   - DB 저장값 + JSON 직렬화는 SF 한국어 원본 (`displayName`)
 *   - JPA 매핑은 `FreezerTypeConverter` 경유
 */
enum class FreezerType(
    val displayName: String
) {
    LARGE("대"),
    MEDIUM("중");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): FreezerType =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 냉장고종류: $value")

        fun fromDisplayNameOrNull(value: String?): FreezerType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
