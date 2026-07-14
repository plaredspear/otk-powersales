package com.otoki.powersales.domain.activity.claim.dto.response

/**
 * Web admin 클레임 등록 / SF 재전송 응답 — Spec #829.
 *
 * - [sfSendStatus] : 신규→SF 전송상태([com.otoki.powersales.domain.activity.claim.enums.ClaimSfSendStatus]).
 *   등록 직후엔 "PENDING" (SF 송신은 커밋 후 비동기). 재전송 시엔 동기 전송 결과가 반영된 "SENT" 또는
 *   "SEND_FAILED". (코스모스 전송상태인 claim.status 와는 다른 축.)
 * - [sfResultCode] : SF `RESULT_CODE` ("200" 또는 "0"). 등록 응답에선 SF 송신 전이므로 null,
 *   재전송 응답에선 동기 SF 호출 결과. HTTP 오류 등으로 응답 자체 부재 시 null.
 * - [sfResultMsg]  : SF `RESULT_MSG` (HTTP 오류 시 backend 오류 요약). 등록 응답에선 null.
 */
data class AdminClaimCreateResponse(
    val claimId: Long,
    val sfSendStatus: String,
    val sfResultCode: String? = null,
    val sfResultMsg: String? = null,
)
