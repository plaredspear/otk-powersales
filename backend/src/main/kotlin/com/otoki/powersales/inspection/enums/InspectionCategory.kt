package com.otoki.powersales.inspection.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * 현장점검 분류 — mobile `category` 계약 enum.
 *
 * SF `DKRetail__SiteAcitivity__c.DKRetail__ProductType__c` picklist (자사 / 경쟁사) 매핑.
 * - JSON (mobile 계약): `OWN` / `COMPETITOR`
 * - storedValue (entity.productType / SF picklist): `자사` / `경쟁사`
 */
enum class InspectionCategory(
    val storedValue: String
) {
    OWN("자사"),
    COMPETITOR("경쟁사");

    @JsonValue
    fun toJson(): String = name

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromJson(value: String): InspectionCategory =
            entries.find { it.name == value }
                ?: throw IllegalArgumentException("유효하지 않은 InspectionCategory: $value")

        /** SF picklist 저장값(자사/경쟁사) → enum. 미매칭/blank 는 null. */
        fun fromStoredValue(value: String?): InspectionCategory? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.storedValue == value }
        }
    }
}
