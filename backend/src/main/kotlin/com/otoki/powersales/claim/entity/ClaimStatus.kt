package com.otoki.powersales.claim.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `DKRetail__Claim__c.DKRetail__Status__c` (상태) picklist enum.
 *
 * 단일 권위: docs/plan/old_source_260408/sf-object-meta/DKRetail__Claim__c.md (선택 목록 3개)
 *
 * Spec #705 Q4 결정:
 *   - SF 옵션값으로 정합 (DRAFT/SENT/SEND_FAILED) — 기존 ClaimStatus 4개 (SUBMITTED/IN_PROGRESS/RESOLVED/REJECTED) 폐기
 *   - DB 저장값 + JSON 직렬화는 SF 한국어 원본 (`displayName`)
 *   - JPA 매핑은 `ClaimStatusConverter` 경유
 *   - dev 환경 기존 데이터 삭제 (사용자 결정)
 */
enum class ClaimStatus(
    val displayName: String
) {
    DRAFT("임시저장"),
    SENT("전송완료"),
    SEND_FAILED("전송실패");

    @JsonValue
    fun toJson(): String = displayName

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromDisplayName(value: String): ClaimStatus =
            entries.find { it.displayName == value }
                ?: throw IllegalArgumentException("유효하지 않은 클레임 상태: $value")

        fun fromDisplayNameOrNull(value: String?): ClaimStatus? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.displayName == value }
        }
    }
}
