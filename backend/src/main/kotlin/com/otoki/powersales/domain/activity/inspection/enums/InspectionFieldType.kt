package com.otoki.powersales.domain.activity.inspection.enums

/**
 * 현장유형 — mobile `fieldType` / `fieldTypeCode` 계약 enum.
 *
 * SF `DKRetail__SiteAcitivity__c.DKRetail__Category__c` picklist (본매대 / 행사매대 / 시식 / 기타) 매핑.
 * - code (mobile fieldTypeCode): enum name (`MAIN_SHELF` 등)
 * - displayName (mobile fieldType / entity.category / SF picklist): 한글값
 */
enum class InspectionFieldType(
    val displayName: String
) {
    MAIN_SHELF("본매대"),
    EVENT_SHELF("행사매대"),
    TASTING("시식"),
    ETC("기타");

    companion object {
        /** code(enum name) → enum. 미매칭/blank 는 null. */
        fun fromCode(value: String?): InspectionFieldType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name == value }
        }

        /** SF picklist 저장값(본매대 등) → enum. 미매칭/blank 는 null. */
        fun fromStoredValue(value: String?): InspectionFieldType? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
