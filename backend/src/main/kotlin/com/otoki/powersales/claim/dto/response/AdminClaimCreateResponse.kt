package com.otoki.powersales.claim.dto.response

/**
 * Web admin 클레임 등록 응답 — Spec #829.
 *
 * SF push 결과를 그대로 전달하되 backend.claim 의 적재 사실은 status 와 무관하게 보존.
 * - [status] : "SENT" (SF 성공) 또는 "SEND_FAILED" (SF 실패)
 * - [sfResultCode] : SF `RESULT_CODE` ("200" 또는 "0"). HTTP 오류 등으로 응답 자체 부재 시 null.
 * - [sfResultMsg]  : SF `RESULT_MSG`. HTTP 오류 시에는 backend 에서 생성한 오류 요약.
 */
data class AdminClaimCreateResponse(
    val claimId: Long,
    val status: String,
    val sfResultCode: String?,
    val sfResultMsg: String?,
)
