package com.otoki.powersales.domain.activity.claim.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus

/**
 * SF `IF_SendClaimToPWS` Response 한 건의 raw 역직렬화 표현 ("알라딘 클레임 마스터 API" 문서 정합).
 *
 * SF → PWS 방향 조회 응답 항목 1건. 응답에는 32개 필드가 오지만, 신규가 회수하는 것은 **조치 6필드 + 코스모스
 * 전송상태(status) 7개** 다. 그 외 등록 시 확정 필드(제품/거래처/수량 등) 는 갱신 대상이 아니므로 본 레코드에
 * 매핑하지 않는다. 문서 외 필드가 와도 무시하도록 [JsonIgnoreProperties] 를 둔다.
 *
 * ## 조치 6필드 — SF 레거시 inbound Apex 두 개가 claim 을 update 로 set 하는 필드의 합집합(권위 출처)
 *  - `IF_ClaimStatusUpdate` (`/if_claimstatusupdate/`): actionStatus·actionCode·counselNumber·reasonType·actContent 5개.
 *  - `IF_REST_SAP_ClaimReceive` (`/sap/ClaimReceive/`): 위 5개 + cosmosKey (즉 cosmosKey 는 이 API 에서만 set).
 * 두 API 모두 매칭 키는 claim `Name`(접수번호/EXNUM).
 *
 * ## status(코스모스 전송상태) — 신규 시스템의 유일한 회수 경로
 * `DKRetail__Status__c` 는 SF 가 **자기 안에서** 전이시키는 값이다: `/ClaimRegist`(`IF_REST_MOBILE_ClaimRegist`)
 * 는 '임시저장' 으로 insert 하고, 첨부 CDL insert 가 `ContentDocumentLinkAfterTrigger` → `MobileClaimQueueable`
 * → `SendClaimController.sendClaim` 을 태워 코스모스 전송 성공(CODE=201) 시 '전송완료'(실패 시 '전송실패') 로
 * 바꾼다. 신규 시스템에는 이 전이를 관측할 다른 경로가 없어, 본 sync 가 값을 가져오지 않으면 앱/웹 등록분이
 * 영구히 '임시저장' 으로 남는다(기간별 클레임 보고서가 status='전송완료' 만 집계하므로 전량 누락됐다).
 *
 * 주의: 응답 스키마에 있는 `ActionDate`(조치일시)·`ClaimSequence`(COSMOS 상담번호) 는 레거시 Apex 가 claim 에
 * write 하지 않으므로(SELECT 조회에만 사용) 본 레코드에도 매핑하지 않는다. `IF_REST_SAP_ClaimReceive` 는 SAP
 * 페이로드의 `ClaimSequence` 값을 `ClaimSequence__c` 가 아니라 counselNumber(상담번호) 로 저장한다.
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
 * @property cosmosKey    COSMOS 접수번호 → Claim.cosmosKey. 레거시상 `IF_REST_SAP_ClaimReceive` 에서만 set
 *                        (`IF_ClaimStatusUpdate` 는 미갱신) — 문서 응답 스키마에 없을 수 있어 null 허용.
 * @property status       코스모스 전송상태(`DKRetail__Status__c`, 한국어 원본 임시저장/전송완료/전송실패)
 *                        → [com.otoki.powersales.domain.activity.claim.entity.Claim.status]. [statusAsClaimStatus] 로 파싱.
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
    @JsonProperty("Status") val status: String? = null,
) {

    /** pwrskey 를 claim PK(Long) 로 파싱. 비어있거나 숫자가 아니면 null (PK 매칭 불가 — name fallback 대상). */
    fun pwrskeyAsClaimId(): Long? = pwrskey?.takeIf { it.isNotBlank() }?.trim()?.toLongOrNull()

    /** name(접수번호) 을 매칭 키로 정규화. 비어있으면 null (매칭 불가). */
    fun nameAsClaimName(): String? = name?.trim()?.takeIf { it.isNotBlank() }

    /**
     * Status 를 [ClaimStatus] 로 파싱. 비었거나 picklist 3값(임시저장/전송완료/전송실패) 밖이면 null.
     *
     * null 은 "갱신하지 않음" 을 뜻한다 — 응답에 Status 가 빠진 레코드까지 덮으면 SF 가 이미 '전송완료' 로
     * 올린 건이 다시 '임시저장' 으로 되돌아간다(cosmosKey 와 달리 무조건 덮어쓰지 않는 이유).
     */
    fun statusAsClaimStatus(): ClaimStatus? = ClaimStatus.fromDisplayNameOrNull(status)
}
