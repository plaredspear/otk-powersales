package com.otoki.powersales.domain.foundation.account.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `Account.Ownership` (소유권) picklist enum.
 *
 * 단일 권위: Salesforce Object 메타 (`Account`) (선택 목록 4개)
 *
 * Spec #703 §3-2 결정:
 *   - enum name 은 SF 옵션값의 Java 식별자 변환 (UPPER_SNAKE_CASE)
 *   - `displayName` 에 SF 원본 옵션값 보존 (영어 그대로)
 *   - DB 저장값 + JSON 직렬화는 `displayName`
 *   - JPA 매핑은 `OwnershipConverter` 경유
 */
enum class Ownership(
    val displayName: String
) {
    PUBLIC("Public"),
    PRIVATE("Private"),
    SUBSIDIARY("Subsidiary"),
    OTHER("Other");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): Ownership =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 소유권: $value")

        fun fromDisplayNameOrNull(value: String?): Ownership? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
