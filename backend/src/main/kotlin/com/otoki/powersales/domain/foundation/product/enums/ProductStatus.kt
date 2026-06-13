package com.otoki.powersales.domain.foundation.product.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `DKRetail__Product__c.DKRetail__ProductStatus__c` picklist enum.
 *
 * SF picklist 운영값이 placeholder `-` 1개만 정의된 상태 — 운영 picklist 마스터가
 * 마이그레이션 진행 중일 가능성. §6.6 "예외 없이 enum + Converter" 정책에 따라
 * 단일 상수로 enum 형식만 도입하고, SF picklist 옵션 확장 시점에 상수 추가.
 */
enum class ProductStatus(
    val displayName: String
) {
    PLACEHOLDER("-");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): ProductStatus =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 제품 상태: $value")

        fun fromDisplayNameOrNull(value: String?): ProductStatus? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
