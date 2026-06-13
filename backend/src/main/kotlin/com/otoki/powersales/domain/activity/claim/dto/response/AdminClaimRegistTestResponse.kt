package com.otoki.powersales.domain.activity.claim.dto.response

/**
 * SF ClaimRegist 전송 테스트 응답 (개발자 도구 — 외부 API 테스트).
 *
 * 신규 DB(claim 테이블)에는 저장하지 않고 SF Apex REST `/ClaimRegist` 로만 전송하는
 * 순수 전송 테스트 결과. 운영자가 SF 호출 계약을 직접 검증할 수 있도록:
 *  - [requestPayload] : SF 로 전송한 apiMap JSON (이미지 Buffer 는 길이만 표기로 마스킹).
 *  - [success] / [resultCode] / [resultMsg] / [rawResponse] : SF 응답 원형.
 */
data class AdminClaimRegistTestResponse(
    val success: Boolean,
    val resultCode: String?,
    val resultMsg: String?,
    val rawResponse: String?,
    val requestPayload: String,
)
