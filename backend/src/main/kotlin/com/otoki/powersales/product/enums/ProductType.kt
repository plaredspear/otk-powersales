package com.otoki.powersales.product.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `DKRetail__Product__c.DKRetail__ProductType__c` picklist enum (제품 유형).
 *
 * 단일 권위: Salesforce describe 메타 (`DKRetail__Product__c`) picklistValues — `1`, `2`
 * Spec #754 §3.1 — 점프적 enum (의미 표기 부재).
 *
 * displayName 은 SF 옵션값 (`"1"`, `"2"`) 그대로 사용한다.
 * 운영 의미(전용/범용) 는 SF Apex 와 Heroku JS 매핑이 정반대로 충돌하여 본 스펙 시점 확정 불가.
 * 운영 의미 확정 후속은 별도 스펙으로 이관 (Spec #754 §3.4).
 */
enum class ProductType(
    val displayName: String
) {
    PRODUCT_TYPE_1("1"),
    PRODUCT_TYPE_2("2");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): ProductType =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 제품 유형: $value")

        fun fromDisplayNameOrNull(value: String?): ProductType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
