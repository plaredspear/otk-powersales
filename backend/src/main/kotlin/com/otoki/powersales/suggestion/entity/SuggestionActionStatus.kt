package com.otoki.powersales.suggestion.entity

/**
 * 제안 조치상태 Enum.
 *
 * SF Picklist `DKRetail__Proposal__c.ActionStatus__c` 의 4값과 정합 (default = UNCONFIRMED).
 *
 * 조치 흐름 (ActionContent / Manager / Num) 은 별 스펙 위임. 본 스펙은 BR3 (중복접수 시 duplicate_proposal_num 필수)
 * 검증을 위해 enum + 컬럼만 정의.
 */
enum class SuggestionActionStatus(val displayName: String) {
    UNCONFIRMED("미확인"),
    IN_PROGRESS("조치중"),
    COMPLETED("조치 완료"),
    DUPLICATE_RECEPTION("중복접수");

    companion object {
        fun fromDisplayNameOrNull(value: String?): SuggestionActionStatus? =
            value?.let { v -> entries.firstOrNull { it.displayName == v } }
    }
}
