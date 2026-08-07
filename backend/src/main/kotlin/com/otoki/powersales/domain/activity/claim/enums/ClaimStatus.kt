package com.otoki.powersales.domain.activity.claim.enums

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/**
 * Salesforce `DKRetail__Claim__c.DKRetail__Status__c` (상태) picklist enum.
 *
 * **의미**: SF → 외부(코스모스) 전송상태. 마이그레이션 시 SF 값 그대로 적재되며 표시/보고서 필터 전용이다.
 * 신규 시스템 → SF 전송상태는 별도 [ClaimSfSendStatus] (sfSendStatus 컬럼) 가 담당한다 — 두 축은 별개 차원.
 *
 * 단일 권위: Salesforce Object 메타 (`DKRetail__Claim__c`) picklist (임시저장/전송완료/전송실패 3개)
 *
 * Spec #705 Q4 결정:
 *   - SF 옵션값으로 정합 (DRAFT/SENT/SEND_FAILED) — 기존 ClaimStatus 4개 (SUBMITTED/IN_PROGRESS/RESOLVED/REJECTED) 폐기
 *   - DB 저장값 + JSON 직렬화는 SF 한국어 원본 (`displayName`)
 *   - JPA 매핑은 `ClaimStatusConverter` 경유
 *   - dev 환경 기존 데이터 삭제 (사용자 결정)
 *
 * **[displayName] 은 저장/전송값이라 불변**: DB 컬럼(`claim.status`)에 이 문자열이 그대로 들어가고
 * SF `DKRetail__Status__c` 는 restricted picklist (임시저장/전송완료/전송실패) 라 다른 값을 보내면
 * outbound DML 이 거부된다.
 *
 * **모바일 화면에는 이 축을 표시하지 않는다**: 신규 시스템에는 DRAFT 를 벗어나는 전이 경로가 없어
 * (전이는 SF `SendClaimController`/`ClaimTriggerHandler` 안에서만 일어나고 마스터 sync 는 조치 필드만
 * 가져온다) 앱 등록분은 영구히 "임시저장" 으로 남는다. 사원 화면 상태 표시는 실제로 갱신되는
 * `Claim.actionStatus` (코스모스 조치상태) 축을 쓴다 — [com.otoki.powersales.domain.activity.claim.entity.Claim.actionStatusLabel].
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
