package com.otoki.powersales.claim.entity.sfpicklist

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * SF picklist `DKRetail__Claim__c.DKRetail__PurchaseMethod__c` enum (3 옵션 — 법인카드/개인카드/현금).
 *
 * label (한국어) 과 value (A/B/C) 가 분리된 picklist.
 * DB 에는 SF value (단일 문자) 저장, application 에서는 enum 사용.
 * SF 원본 옵션값을 보존 — README §6.6 v2.2 정책 준수.
 */
enum class PurchaseMethod(
    val displayName: String,
    val sfValue: String
) {
    CORPORATE_CARD("법인카드", "A"),
    PERSONAL_CARD("개인카드", "B"),
    CASH("현금", "C");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): PurchaseMethod =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 구매방법: $value")

        fun fromDisplayNameOrNull(value: String?): PurchaseMethod? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }

        fun fromSfValueOrNull(value: String?): PurchaseMethod? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.sfValue == value }
        }
    }
}
