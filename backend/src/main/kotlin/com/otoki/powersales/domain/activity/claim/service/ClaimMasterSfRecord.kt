package com.otoki.powersales.domain.activity.claim.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SF `IF_SendClaimToPWS` Response 한 건의 raw 역직렬화 표현 ("알라딘 클레임 마스터 API" 문서 정합).
 *
 * SF → PWS 방향 조회 응답 항목 1건. 응답에는 32개 필드가 오지만, 신규는 **SF 레거시 inbound Apex
 * (`IF_ClaimStatusUpdate` / `IF_REST_SAP_ClaimReceive`) 가 claim 을 update 로 set 하는 필드 집합** 만
 * 갱신한다(외부→claim 갱신 컬럼의 권위 출처). 그 외 등록 시 확정 필드(제품/거래처/수량 등) 는 갱신 대상이
 * 아니므로 본 레코드에 매핑하지 않는다. 문서 외 필드가 와도 무시하도록 [JsonIgnoreProperties] 를 둔다.
 *
 * @property pwrskey      신규 claim 의 primary key(`claim_id`) — 신규 시스템에서 생성한 claim 을 SF 가 echo.
 *                        존재하면 이 값(claim_id)으로 claim 을 우선 조회해 매칭한다.
 * @property name         SF 표준 Name(접수번호, EXNUM) — SF 에서 생성한 클레임의 자연키. pwrskey 가 없을 때
 *                        (SF 단독 생성분) 이 값으로 [com.otoki.powersales.domain.activity.claim.entity.Claim.name] 을 조회해 매칭한다.
 * @property actionStatus 조치 상태 → [com.otoki.powersales.domain.activity.claim.entity.Claim.actionStatus]
 * @property actionCode   조치 코드 → Claim.actionCode
 * @property counselNumber 상담번호 → Claim.counselNumber
 * @property reasonType   사유 유형(원인별 분류) → Claim.reasonType
 * @property actContent   조치 내용 → Claim.actContent
 * @property cosmosKey    COSMOS 접수번호 → Claim.cosmosKey (문서 응답 스키마에 없을 수 있어 null 허용)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ClaimMasterSfRecord(
    @JsonProperty("pwrskey") val pwrskey: String? = null,
    @JsonProperty("Name") val name: String? = null,
    @JsonProperty("ActionStatus") val actionStatus: String? = null,
    @JsonProperty("ActionCode") val actionCode: String? = null,
    @JsonProperty("counselNumber") val counselNumber: String? = null,
    @JsonProperty("ReasonType") val reasonType: String? = null,
    @JsonProperty("ActContent") val actContent: String? = null,
    @JsonProperty("CosmosKey") val cosmosKey: String? = null,
) {

    /** pwrskey 를 claim PK(Long) 로 파싱. 비어있거나 숫자가 아니면 null (PK 매칭 불가 — name fallback 대상). */
    fun pwrskeyAsClaimId(): Long? = pwrskey?.takeIf { it.isNotBlank() }?.trim()?.toLongOrNull()

    /** name(접수번호) 을 매칭 키로 정규화. 비어있으면 null (매칭 불가). */
    fun nameAsClaimName(): String? = name?.trim()?.takeIf { it.isNotBlank() }
}
