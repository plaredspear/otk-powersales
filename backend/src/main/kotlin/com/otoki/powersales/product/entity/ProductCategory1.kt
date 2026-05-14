package com.otoki.powersales.product.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `DKRetail__Product__c.DKRetail__Category1__c` picklist enum.
 *
 * SF picklist 운영값이 placeholder `-` 1개만 정의된 상태. §6.6 정책상 enum + Converter
 * 형식 도입. SF picklist 옵션 확장 시점에 상수 추가.
 */
enum class ProductCategory1(
    val displayName: String
) {
    PLACEHOLDER("-");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): ProductCategory1 =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 카테고리1: $value")

        fun fromDisplayNameOrNull(value: String?): ProductCategory1? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
