package com.otoki.powersales.domain.foundation.account.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `Account.AccountSource` (계정 소스) picklist enum.
 *
 * 단일 권위: Salesforce Object 메타 (`Account`) (선택 목록 10개)
 *
 * Spec #703 §3-2 결정:
 *   - enum name 은 SF 옵션값의 Java 식별자 변환 (공백 → `_`, UPPER_SNAKE_CASE)
 *   - `displayName` 에 SF 원본 옵션값 보존 (영어 그대로)
 *   - DB 저장값 + JSON 직렬화는 `displayName`
 *   - JPA 매핑은 `AccountSourceConverter` 경유
 */
enum class AccountSource(
    val displayName: String
) {
    ADVERTISEMENT("Advertisement"),
    CUSTOMER_EVENT("Customer Event"),
    EMPLOYEE_REFERRAL("Employee Referral"),
    GOOGLE_ADWORDS("Google AdWords"),
    OTHER("Other"),
    PARTNER("Partner"),
    PURCHASED_LIST("Purchased List"),
    TRADE_SHOW("Trade Show"),
    WEBINAR("Webinar"),
    WEBSITE("Website");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): AccountSource =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 계정 소스: $value")

        fun fromDisplayNameOrNull(value: String?): AccountSource? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
