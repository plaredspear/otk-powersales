package com.otoki.powersales.account.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `Account.Type` (거래처유형) picklist enum.
 *
 * 단일 권위: Salesforce Object (`Account`) (선택 목록 14개)
 *
 * Spec #602 Q5/Q5-1 결정:
 *   - enum name 은 영문 (컨벤션 보존)
 *   - DB 저장값 + JSON 직렬화는 SF 한국어 원본 (`displayName`)
 *   - JPA 매핑은 `AccountTypeConverter` 경유
 *   - 기존 SAP 인바운드 적재 데이터의 한국어 값 그대로 호환 — 백필 불필요
 */
enum class AccountType(
    val displayName: String
) {
    DISCOUNT_STORE("할인점"),
    CHAIN("체인"),
    NONGHYUP("농협"),
    SUPER("수퍼"),
    FOOD_MATERIAL("식자재"),
    GROUP_FEEDING("단체급식"),
    OIL_CONFECTIONERY("유지제과"),
    RESTAURANT("외식"),
    DEPARTMENT_STORE("백화점"),
    CVS("C.V.S"),
    AGENCY("대리점"),
    MANUFACTURING("제조"),
    MILITARY("군납"),
    OTHER("기타");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): AccountType =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 거래처유형: $value")

        fun fromDisplayNameOrNull(value: String?): AccountType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
