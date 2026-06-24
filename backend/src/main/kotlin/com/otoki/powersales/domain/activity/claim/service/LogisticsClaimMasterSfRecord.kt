package com.otoki.powersales.domain.activity.claim.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.otoki.powersales.domain.activity.suggestion.entity.SuggestionActionStatus

/**
 * SF `IF_SendLogisticsClaimToPWS` Response 한 건의 raw 역직렬화 표현 ("알라딘 물류클레임 마스터 API" 문서 정합).
 *
 * SF → PWS 방향 조회 응답 항목 1건. 물류 클레임은 신규에서 제안(`Suggestion`, `DKRetail__Proposal__c`) 도메인이다.
 *
 * ## 갱신 대상 = 조치 계열 6필드
 * SF 레거시에는 일반 클레임(`IF_ClaimStatusUpdate` / `IF_REST_SAP_ClaimReceive`)과 달리 **물류클레임을 외부에서
 * 갱신하는 inbound Apex 가 없다** — 조치 필드는 SF 내부 담당자가 UI 에서 직접 입력하고 모바일은 조회로 읽기만 했다.
 * 따라서 갱신 대상 필드의 권위 출처는 조회 REST(`IF_REST_MOBILE_LogisticsClaimSearch`)의 응답 payload 중
 * **등록 시 set 되지 않고 나중에 채워지는 조치 계열 필드** 6개다:
 *   actionNum / actionStatus / actionManager / logisticsResponsibility / claimTypeMeasures / actionContent.
 * 제목/상세내용/거래처/제품 등 등록 시 확정 필드는 갱신하지 않는다. 문서 외 필드는 무시한다([JsonIgnoreProperties]).
 *
 * @property pwrskey                신규 제안의 primary key(`suggestion_id`) — SF 가 echo. 이 값으로 suggestion 매칭.
 * @property actionNum              조치번호 → [com.otoki.powersales.domain.activity.suggestion.entity.Suggestion.actionNum]
 * @property actionStatus           조치상태 (displayName 문자열) → Suggestion.actionStatus ([SuggestionActionStatus])
 * @property actionManager          조치 담당자 → Suggestion.actionManager
 * @property logisticsResponsibility 물류 책임 구분 → Suggestion.logisticsResponsibility
 * @property claimTypeMeasures      클레임 항목(조치사항) → Suggestion.claimTypeMeasures
 * @property actionContent          조치 내용 → Suggestion.actionContent
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class LogisticsClaimMasterSfRecord(
    @JsonProperty("pwrskey") val pwrskey: String? = null,
    @JsonProperty("ActionNum") val actionNum: String? = null,
    @JsonProperty("ActionStatus") val actionStatus: String? = null,
    @JsonProperty("ActionManager") val actionManager: String? = null,
    @JsonProperty("LogisticsResponsibility") val logisticsResponsibility: String? = null,
    @JsonProperty("ClaimTypeMeasures") val claimTypeMeasures: String? = null,
    @JsonProperty("ActionContent") val actionContent: String? = null,
) {

    /** pwrskey 를 suggestion PK(Long) 로 파싱. 비어있거나 숫자가 아니면 null (매칭 불가). */
    fun pwrskeyAsSuggestionId(): Long? = pwrskey?.takeIf { it.isNotBlank() }?.trim()?.toLongOrNull()

    /** 조치상태 displayName(예: "조치 완료") → enum. 미지정/미매칭이면 null. */
    fun actionStatusEnum(): SuggestionActionStatus? = SuggestionActionStatus.fromDisplayNameOrNull(actionStatus)
}
