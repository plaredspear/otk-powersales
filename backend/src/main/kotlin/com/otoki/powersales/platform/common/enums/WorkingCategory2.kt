package com.otoki.powersales.platform.common.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `StaffReview__c.DKRetail_WorkingCategory2__c` picklist enum.
 *
 * 단일 권위: Salesforce describe 메타 (`StaffReview__c`) picklistValues
 * Spec #711 §6 — 예외 없이 SF picklist 정의 그대로 enum 변환.
 */
enum class WorkingCategory2(
    val displayName: String
) {
    DEDICATED("전담"),
    DISPLAY_CONCURRENT("진열겸임");
    // TEMPORARY("임시") 옵션 제거 — SF picklist 정합. 레거시 Apex/Aura 모든 차원에서 사용 0건 확인 (sf-align-teammemberschedule #762). 본 entity 측 `mapTypeOfWork5ToCategory2("임시")` 는 NULL 반환으로 변경.

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): WorkingCategory2 =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 근무카테고리2: $value")

        fun fromDisplayNameOrNull(value: String?): WorkingCategory2? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
