package com.otoki.powersales.account.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `Account.Industry` (업종) picklist enum.
 *
 * 단일 권위: docs/plan/old_source_260408/sf-object-meta/Account.md (선택 목록 32개)
 *
 * Spec #703 §3-2 결정:
 *   - enum name 은 SF 옵션값의 Java 식별자 변환 (`&` → `AND`, 공백 → `_`, UPPER_SNAKE_CASE)
 *   - `displayName` 에 SF 원본 옵션값 보존 (영어 그대로)
 *   - DB 저장값 + JSON 직렬화는 `displayName`
 *   - JPA 매핑은 `IndustryConverter` 경유
 */
enum class Industry(
    val displayName: String
) {
    AGRICULTURE("Agriculture"),
    APPAREL("Apparel"),
    BANKING("Banking"),
    BIOTECHNOLOGY("Biotechnology"),
    CHEMICALS("Chemicals"),
    COMMUNICATIONS("Communications"),
    CONSTRUCTION("Construction"),
    CONSULTING("Consulting"),
    EDUCATION("Education"),
    ELECTRONICS("Electronics"),
    ENERGY("Energy"),
    ENGINEERING("Engineering"),
    ENTERTAINMENT("Entertainment"),
    ENVIRONMENTAL("Environmental"),
    FINANCE("Finance"),
    FOOD_AND_BEVERAGE("Food & Beverage"),
    GOVERNMENT("Government"),
    HEALTHCARE("Healthcare"),
    HOSPITALITY("Hospitality"),
    INSURANCE("Insurance"),
    MACHINERY("Machinery"),
    MANUFACTURING("Manufacturing"),
    MEDIA("Media"),
    NOT_FOR_PROFIT("Not For Profit"),
    OTHER("Other"),
    RECREATION("Recreation"),
    RETAIL("Retail"),
    SHIPPING("Shipping"),
    TECHNOLOGY("Technology"),
    TELECOMMUNICATIONS("Telecommunications"),
    TRANSPORTATION("Transportation"),
    UTILITIES("Utilities");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): Industry =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 업종: $value")

        fun fromDisplayNameOrNull(value: String?): Industry? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
