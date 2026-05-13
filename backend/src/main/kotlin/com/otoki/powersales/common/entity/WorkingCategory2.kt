package com.otoki.powersales.common.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `StaffReview__c.DKRetail_WorkingCategory2__c` picklist enum.
 *
 * 단일 권위: docs/plan/old_source_260408/sf-object-meta/_raw/StaffReview__c.json picklistValues
 * Spec #711 §6 — 예외 없이 SF picklist 정의 그대로 enum 변환.
 */
enum class WorkingCategory2(
    val displayName: String
) {
    DEDICATED("전담"),
    DISPLAY_CONCURRENT("진열겸임"),
    // SF picklist 옵션 외 — 레거시 운영 데이터(WorkingCategory5.TEMPORARY 매핑 — `mapTypeOfWork5ToCategory2`) 호환을 위해 enum 에 포함. SF org picklist 정비 필요.
    TEMPORARY("임시");

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
