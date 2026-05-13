package com.otoki.powersales.claim.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `DKRetail__Claim__c.DKRetail__ClaimType2__c` (클레임 소분류) picklist enum.
 *
 * 권위 출처: `docs/plan/old_source_260408/sf-object-meta/_raw/DKRetail__Claim__c.json:1916-2087`
 * (`dependentPicklist: true`, `controllerName: "DKRetail__ClaimType1__c"`, `restrictedPicklist: true`).
 *
 * Spec #743:
 *   - `value` 는 SF picklist 원본 옵션값 (AA/AB/.../CF/AL) — DB 저장값과 동일
 *   - `label` 은 SF describe 의 한국어 표시명 (서버가 권위 보유)
 *   - `parent` 는 SF describe 의 `validFor` 비트맵 기반 controlling 옵션
 *     (A=gAAA, B=QAAA, C=IAAA)
 *   - JPA 매핑은 `ClaimType2Converter` 경유
 */
enum class ClaimType2(
    val value: String,
    val label: String,
    val parent: ClaimType1
) {
    // ClaimType1.A (포장불량) — 12 옵션
    AA("AA", "내용물[없음/터짐]", ClaimType1.A),
    AB("AB", "누수/누유", ClaimType1.A),
    AC("AC", "라벨[없음/접착불량/찢어짐]", ClaimType1.A),
    AD("AD", "수량부족", ClaimType1.A),
    AE("AE", "용기[찌그러짐/파손/불량]", ClaimType1.A),
    AF("AF", "유통기한[미표시/지워짐/불량]", ClaimType1.A),
    AG("AG", "이종혼입", ClaimType1.A),
    AH("AH", "중량미달", ClaimType1.A),
    AI("AI", "포장지[접착불량/찢어짐/빈포장지]", ClaimType1.A),
    AJ("AJ", "캡[없음/파손]", ClaimType1.A),
    AK("AK", "케이스[접착불량/찌그러짐]", ClaimType1.A),
    AL("AL", "포장불량 - 기타", ClaimType1.A),

    // ClaimType1.B (이물혼입) — 7 옵션
    BA("BA", "금속/유리류", ClaimType1.B),
    BB("BB", "동물성[뼈/털 등]", ClaimType1.B),
    BC("BC", "플라스틱류", ClaimType1.B),
    BD("BD", "벌레류[파리/애벌레 등]", ClaimType1.B),
    BE("BE", "머리카락", ClaimType1.B),
    BF("BF", "검은이물[면이물/기름때 등]", ClaimType1.B),
    BG("BG", "이물혼입 - 기타", ClaimType1.B),

    // ClaimType1.C (내용물이상) — 6 옵션
    CA("CA", "이미.이취.쩐내 등", ClaimType1.C),
    CB("CB", "성형불량/색상/점도 등", ClaimType1.C),
    CC("CC", "내용물[없음/터짐/분리/마름 등]", ClaimType1.C),
    CD("CD", "팽창", ClaimType1.C),
    CE("CE", "곰팡이", ClaimType1.C),
    CF("CF", "내용물이상 - 기타", ClaimType1.C);

    @JsonValue
    fun toJson(): String = value

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): ClaimType2 =
            entries.find { it.value == value }
                ?: throw IllegalArgumentException("유효하지 않은 클레임 소분류: $value")

        fun fromValueOrNull(value: String?): ClaimType2? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.value == value }
        }
    }
}
