package com.otoki.powersales.domain.activity.claim.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * SF `IF_SendClaimToPWS` Response 한 건의 raw 역직렬화 표현 ("알라딘 클레임 마스터 API" 문서 정합).
 *
 * SF → PWS 방향 조회 응답 항목 1건. 응답 32필드(문서 Response 표) 중 신규가 회수하는 것은 아래 두 부류뿐이다.
 * 나머지(제품/거래처/수량/금액/기한일 등)는 **등록 시 신규가 확정한 값**이라 회수하면 사원 입력이 SF 값으로
 * 훼손되므로 매핑하지 않는다(사원명/거래처명/제품명/직위/휴대전화 등은 신규가 조인으로 더 정확히 안다).
 * 문서 외 필드가 와도 무시하도록 [JsonIgnoreProperties] 를 둔다.
 *
 * ## ① 조치 5필드 — 응답값으로 무조건 갱신
 * actionStatus·actionCode·counselNumber·reasonType·actContent. SF 레거시 inbound Apex
 * (`IF_ClaimStatusUpdate` `/if_claimstatusupdate/` · `IF_REST_SAP_ClaimReceive` `/sap/ClaimReceive/`) 가 claim 을
 * update 로 set 하는 필드의 합집합이며(권위 출처), 본 조회 응답은 SF 레코드의 현재값이므로 빈 값이 와도 그대로
 * 반영하는 것이 맞다. 두 레거시 API 의 매칭 키는 claim `Name`(접수번호/EXNUM).
 *
 * ## ② SF 만 아는 값 6종 — **값이 온 경우에만** 갱신
 * status·name·interfaceDate·logisticsCenter·sampleCollectionFlag·cosmosKey. 신규는 등록 시 이 컬럼들을 채우지
 * 않고([com.otoki.powersales.domain.activity.claim.entity.Claim.forRegistration]) 이후에도 쓰지 않으므로, 본 sync
 * 가 유일한 공급원이다. 응답에 값이 없다고 덮으면 이미 채워둔 값(마이그레이션 적재분 포함)이 소실되므로
 * null/빈값은 "갱신하지 않음" 으로 처리한다.
 *  - `Name`(접수번호) : SF auto-number. `/ClaimRegist` 응답은 `{RESULT_CODE, RESULT_MSG}` 뿐이라 등록 시 회신
 *    경로가 없어, 앱/웹 등록분은 이 sync 전까지 접수번호가 공란이었다(모바일 목록·상세 `claimNo`, 기간별 보고서).
 *  - `InterfaceDate`(전송일시) : SF `SendClaimController` 가 코스모스 전송 성공 시각으로 기록.
 *  - `LogisticsCenter`(출고처) : SF 가 등록 시 `IF_REST_SAP_InventorySearch` 로 채우는 물류센터.
 *  - `SampleCollectionFlag`(샘플 회수 여부) : 상담실/SF 측 결정값. 응답은 `"true"`/`"false"` 문자열.
 *
 * ## status(코스모스 전송상태) — 신규 시스템의 유일한 회수 경로
 * `DKRetail__Status__c` 는 SF 가 **자기 안에서** 전이시키는 값이다: `/ClaimRegist`(`IF_REST_MOBILE_ClaimRegist`)
 * 는 '임시저장' 으로 insert 하고, 첨부 CDL insert 가 `ContentDocumentLinkAfterTrigger` → `MobileClaimQueueable`
 * → `SendClaimController.sendClaim` 을 태워 코스모스 전송 성공(CODE=201) 시 '전송완료'(실패 시 '전송실패') 로
 * 바꾼다. 신규 시스템에는 이 전이를 관측할 다른 경로가 없어, 본 sync 가 값을 가져오지 않으면 앱/웹 등록분이
 * 영구히 '임시저장' 으로 남는다(기간별 클레임 보고서가 status='전송완료' 만 집계하므로 전량 누락됐다).
 *
 * ## 응답에 없는 필드 (문서 Response 표 32필드 기준)
 * `CosmosKey`·`ActionDate`(조치일시)·`ClaimSequence`(COSMOS 상담번호) 는 **응답 스키마에 없다**. 특히 cosmosKey 는
 * 레거시 `IF_REST_SAP_ClaimReceive` 가 SAP 인바운드 페이로드로 받아 set 하던 값이라 본 조회 API 와는 무관하다 —
 * 무조건 대입하면 마이그레이션으로 적재한 `cosmos_key`(`_migration.sf.stage1.Stage1Targets` 매핑)가 sync 마다
 * NULL 로 지워지므로 ②의 조건부 갱신에 포함시킨다. 매핑 자체는 남겨 두어 SF 가 나중에 필드를 추가하면 자동
 * 회수되게 한다.
 *
 * `ReturnOrderNumber`(반품 오더번호)·`division`(부문) 은 응답에 오지만 아직 회수하지 않는다. 전자는 SF 에
 * `ReturnOrderNumber__c`(신규 `customer_delivery_date`, 실제 의미 '거래처납품일자') 와
 * `DKRetail__ReturnOrderNumber__c`(신규 `return_order_number`) 두 필드가 있어 어느 쪽인지 실데이터 확인이 필요하고,
 * 후자는 레거시도 CAP 등록분이 `ClaimTrigger` 의 Interface 유저 가드로 공란이라 실익이 없다.
 *
 * @property pwrskey      신규 claim 의 primary key(`claim_id`) — 신규 시스템에서 생성한 claim 을 SF 가 echo.
 *                        존재하면 이 값(claim_id)으로 claim 을 우선 조회해 매칭한다.
 * @property name         SF 표준 Name(접수번호, EXNUM) — SF 에서 생성한 클레임의 자연키. pwrskey 가 없을 때
 *                        (SF 단독 생성분) 이 값으로 [com.otoki.powersales.domain.activity.claim.entity.Claim.name] 을 조회해 매칭하며,
 *                        pwrskey 로 매칭된 신규 등록분에는 이 값을 접수번호로 적재한다.
 * @property actionStatus 조치 상태 → [com.otoki.powersales.domain.activity.claim.entity.Claim.actionStatus]
 * @property actionCode   조치 코드 → Claim.actionCode
 * @property counselNumber 상담번호 → Claim.counselNumber
 * @property reasonType   사유 유형(원인별 분류) → Claim.reasonType
 * @property actContent   조치 내용 → Claim.actContent
 * @property cosmosKey    COSMOS 접수번호 → Claim.cosmosKey. 문서 Response 표에 없는 필드라 통상 null 로 온다
 *                        (조건부 갱신 — 위 "응답에 없는 필드" 참조).
 * @property status       코스모스 전송상태(`DKRetail__Status__c`, 한국어 원본 임시저장/전송완료/전송실패)
 *                        → [com.otoki.powersales.domain.activity.claim.entity.Claim.status]. [statusAsClaimStatus] 로 파싱.
 * @property interfaceDate 전송일시(`yyyy-MM-dd HH:mm:ss`) → Claim.interfaceDate. [interfaceDateAsDateTime] 로 파싱.
 * @property logisticsCenter 출고처/물류센터 → Claim.logisticsCenter.
 * @property sampleCollectionFlag 샘플 회수 여부(`"true"`/`"false"` 문자열) → Claim.sampleCollectionFlag.
 *                        [sampleCollectionFlagAsBoolean] 로 파싱.
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
    @JsonProperty("InterfaceDate") val interfaceDate: String? = null,
    @JsonProperty("LogisticsCenter") val logisticsCenter: String? = null,
    @JsonProperty("SampleCollectionFlag") val sampleCollectionFlag: String? = null,
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

    /** COSMOS 접수번호 — 비었으면 null(갱신하지 않음). */
    fun cosmosKeyOrNull(): String? = cosmosKey?.trim()?.takeIf { it.isNotBlank() }

    /** 출고처/물류센터 — 비었으면 null(갱신하지 않음). */
    fun logisticsCenterOrNull(): String? = logisticsCenter?.trim()?.takeIf { it.isNotBlank() }

    /**
     * 전송일시 파싱 — SF 응답 형식 `yyyy-MM-dd HH:mm:ss`. ISO(`yyyy-MM-ddTHH:mm:ss`) 도 받아 준다.
     * 비었거나 두 형식 모두 아니면 null (갱신하지 않음).
     */
    fun interfaceDateAsDateTime(): LocalDateTime? {
        val raw = interfaceDate?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return runCatching { LocalDateTime.parse(raw, SF_DATETIME_FORMAT) }
            .recoverCatching { LocalDateTime.parse(raw) }
            .getOrNull()
    }

    /** 샘플 회수 여부 파싱 — `"true"`/`"false"` 문자열. 비었거나 다른 값이면 null (갱신하지 않음). */
    fun sampleCollectionFlagAsBoolean(): Boolean? =
        sampleCollectionFlag?.trim()?.lowercase()?.toBooleanStrictOrNull()

    companion object {
        /** SF 응답 일시 형식 (`"2026-09-07 21:37:19"`). */
        private val SF_DATETIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
