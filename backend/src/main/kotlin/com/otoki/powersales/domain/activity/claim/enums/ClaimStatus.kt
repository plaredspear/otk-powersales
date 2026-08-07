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
 * outbound DML 이 거부된다. 화면 문구를 바꿔야 하면 [mobileLabel] 처럼 표시 전용 라벨을 더한다.
 */
enum class ClaimStatus(
    val displayName: String,
    /**
     * 모바일(영업사원) 화면 표시 라벨. 기본은 [displayName] 과 동일하고, 사원 관점에서 오해를 부르는
     * 값만 따로 지정한다.
     *
     * DRAFT: SF 원본은 "임시저장"(= 코스모스 미전송) 이지만, 사원에게는 ① 이미 접수된 클레임이고
     * ② 클레임 등록 화면의 진짜 임시저장(`claim_draft` 이어쓰기) 과 같은 단어라 혼동을 준다.
     * 그래서 사원 화면에서만 "조치중" 으로 표시한다. 웹 관리자는 재전송 판단에 전송상태 축이
     * 필요하므로 [displayName] 기준 문구를 그대로 쓴다.
     */
    val mobileLabel: String = displayName
) {
    DRAFT("임시저장", mobileLabel = "조치중"),
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
