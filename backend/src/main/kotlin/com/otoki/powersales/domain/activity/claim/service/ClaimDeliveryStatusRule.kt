package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 코스모스 전송상태(`claim.status`) 파생 승격 규칙 — "조치/처리 정보가 있으면 전송완료였다".
 *
 * ## 배경
 * `DKRetail__Status__c` 는 SF 가 자기 안에서 전이시키는 값이다(`/ClaimRegist` 가 '임시저장' 으로 insert →
 * 첨부 CDL 트리거 → `SendClaimController.sendClaim` 이 코스모스 전송 성공 시 '전송완료'). 신규 시스템은 그
 * 전이를 클레임 마스터 sync([AdminClaimMasterSyncTestService]) 로만 관측하는데, sync 가 Status 를 회수하기
 * 시작한 것이 2026-09-08 이고 조회 범위도 `MOD_DT=오늘` 뿐이라 **그 이전 등록분이 전량 '임시저장' 으로
 * 잔류**한다. 기간별 클레임 보고서(SF Report `X3_ONLY_veg`/`X4_3xv` 이식)가 status='전송완료' 만 집계하므로
 * 그 잔류분이 통째로 누락됐다.
 *
 * ## 레거시 근거
 * SF `ClaimTriggerHandler.afterUpdateStatus()` 가 동일 취지의 보정 규칙을 갖고 있다:
 * ```apex
 * if((Status__c == '전송실패' || Status__c == '임시저장') && counselNumber__c != null) Status__c = '전송완료';
 * ```
 * 다만 레거시는 호출부가 주석 처리(`beforeUpdate()` 의 `//afterUpdateStatus();`)돼 **비활성**이고,
 * `ClaimTrigger` 자체가 Interface 유저(코스모스/SAP 인바운드) update 를 bypass 해 어차피 발화하지 않았다.
 * 즉 본 규칙은 "레거시 완전정합" 이 아니라 **레거시가 의도했던 보정의 재활성화(결과정합)** 다.
 *
 * ## 판정 근거 (증거 필드)
 * 아래 값들은 코스모스 전송이 성공한 뒤에야 생기므로, 존재 자체가 '전송완료' 의 역추론 근거가 된다.
 *  - `interfaceDate` : SF `SendClaimController` 가 **코스모스 전송 성공 시각**으로 기록 — 가장 직접적인 증거.
 *  - `counselNumber` : 레거시 규칙이 채택한 신호(코스모스 상담 접수번호).
 *  - `cosmosKey` : 코스모스 접수번호(`IF_REST_SAP_ClaimReceive` 인바운드 적재분).
 *  - `actionStatus` / `actionCode` / `reasonType` / `actContent` : 코스모스·SAP 인바운드로만 채워지는 조치 정보.
 *    sync 가 빈 값도 무조건 덮어쓰는 필드라 `""` 가 저장돼 있을 수 있어 **blank 는 증거로 보지 않는다**.
 *
 * ## 대상 한정 (2026-08-10 커트라인 + 이관분 제외)
 *  - [PROMOTION_START_AT] 이후 **등록(`createdAt`)** 분만 대상 — 운영 판단으로 그 이전 정보는 반영하지 않는다.
 *  - `sfid != null` 인 **SF 이관분은 제외** — 마이그레이션이 SF 원본 Status 를 그대로 적재했으므로 그 행의
 *    '임시저장' 은 실제로 임시저장이다. 추정으로 덮으면 원본을 훼손한다.
 *  - 이미 [ClaimStatus.SENT] 인 행은 무변경.
 *  - `isDeleted` 는 판정에 쓰지 않는다 — 운영 claim 에 `is_deleted IS NULL` 행이 존재해(V60 의 NOT NULL 제약이
 *    운영 스키마에 반영돼 있지 않음) 삭제 조건을 걸면 대상 전건이 탈락한다. 기간별 클레임 보고서 조회도
 *    이 컬럼을 보지 않으므로 조건을 두지 않는 편이 보고서와 정합이다.
 *
 * ## 부작용 (의도된 것)
 * [com.otoki.powersales.domain.activity.claim.service.ClaimService] 의 수정 게이트가 `status != DRAFT` 면
 * 사원의 클레임 수정·사진삭제를 차단한다(레거시 `ClaimTriggerHandler.beforeUpdateClaim`/`beforeDeleteClaim`
 * 정합). 승격된 건은 수정 불가가 되는데, 조치 정보가 붙은 건은 이미 코스모스 접수건이라 차단이 정합이다.
 * SF 재전송은 별도 축(`sfSendStatus`)이라 영향받지 않는다.
 *
 * ## 한계
 * 조치 정보가 아직 안 붙은 최근 전송완료건은 이 규칙으로 구제되지 않는다(접수~조치 회신 시차). 근본 해소는
 * sync 의 Status 회수이며, 본 규칙은 sync 가 조치 필드만 받아오던 구간의 존량을 따라잡는 보정이다.
 */
object ClaimDeliveryStatusRule {

    /** 승격 대상 등록일 커트라인 — 이 시각 이후 `createdAt` 만 반영한다(운영 결정: 2026-08-10). */
    val PROMOTION_START_DATE: LocalDate = LocalDate.of(2026, 8, 10)

    /** [PROMOTION_START_DATE] 의 자정 — `createdAt >=` 비교값. */
    val PROMOTION_START_AT: LocalDateTime = PROMOTION_START_DATE.atStartOfDay()

    /** 승격 가능한 현재 상태 — 레거시 규칙과 동일하게 '임시저장'/'전송실패' 둘 다. (NULL 도 별도로 허용) */
    val PROMOTABLE_STATUSES: List<ClaimStatus> = listOf(ClaimStatus.DRAFT, ClaimStatus.SEND_FAILED)

    /** 코스모스 전송이 실제로 일어났다는 증거가 하나라도 있는가. blank 문자열은 증거로 보지 않는다. */
    fun hasDeliveryEvidence(claim: Claim): Boolean =
        claim.interfaceDate != null ||
            claim.cosmosKey.hasText() ||
            claim.counselNumber.hasText() ||
            claim.actionStatus.hasText() ||
            claim.actionCode.hasText() ||
            claim.reasonType.hasText() ||
            claim.actContent.hasText()

    /** 승격 대상인가 — 이관분 제외 + 커트라인 이후 등록 + 상태 미확정 + 전송 증거 보유. */
    fun isPromotable(claim: Claim): Boolean =
        claim.sfid == null &&
            !claim.createdAt.isBefore(PROMOTION_START_AT) &&
            (claim.status == null || claim.status in PROMOTABLE_STATUSES) &&
            hasDeliveryEvidence(claim)

    /**
     * 대상이면 status 를 [ClaimStatus.SENT] 로 승격한다.
     *
     * @return 실제로 승격했으면 true (집계/로그용). 대상이 아니면 무변경 + false.
     */
    fun promoteIfDelivered(claim: Claim): Boolean {
        if (!isPromotable(claim)) return false
        claim.status = ClaimStatus.SENT
        return true
    }

    private fun String?.hasText(): Boolean = !this.isNullOrBlank()
}
