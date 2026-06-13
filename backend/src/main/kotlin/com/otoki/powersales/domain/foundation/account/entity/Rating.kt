package com.otoki.powersales.domain.foundation.account.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `Account.Rating` (계정 등급) picklist enum.
 *
 * 단일 권위: Salesforce Object 메타 (`Account`) (선택 목록 3개)
 *
 * Spec #703 §3-2 결정:
 *   - enum name 은 SF 옵션값의 Java 식별자 변환 (UPPER_SNAKE_CASE)
 *   - `displayName` 에 SF 원본 옵션값 보존 (영어 그대로)
 *   - DB 저장값 + JSON 직렬화는 `displayName`
 *   - JPA 매핑은 `RatingConverter` 경유
 */
enum class Rating(
    val displayName: String
) {
    HOT("Hot"),
    WARM("Warm"),
    COLD("Cold");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): Rating =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 계정 등급: $value")

        fun fromDisplayNameOrNull(value: String?): Rating? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
