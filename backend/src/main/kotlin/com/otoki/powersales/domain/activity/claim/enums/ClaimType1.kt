package com.otoki.powersales.domain.activity.claim.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `DKRetail__Claim__c.DKRetail__ClaimType1__c` (클레임 대분류) picklist enum.
 *
 * 권위 출처: Salesforce describe 메타 (`DKRetail__Claim__c`):1831-1856`
 * (`restrictedPicklist: true`).
 *
 * Spec #743:
 *   - `value` 는 SF picklist 원본 옵션값 (A/B/C) — DB 저장값과 동일
 *   - `label` 은 SF describe 의 한국어 표시명 (서버가 권위 보유)
 *   - JPA 매핑은 `ClaimType1Converter` 경유
 */
enum class ClaimType1(
    val value: String,
    val label: String
) {
    A("A", "포장불량"),
    B("B", "이물혼입"),
    C("C", "내용물이상");

    @JsonValue
    fun toJson(): String = value

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): ClaimType1 =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("유효하지 않은 클레임 대분류: $value")

        fun fromValueOrNull(value: String?): ClaimType1? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.value == value }
        }
    }
}
