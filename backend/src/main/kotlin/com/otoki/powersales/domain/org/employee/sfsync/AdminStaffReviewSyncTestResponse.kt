package com.otoki.powersales.domain.org.employee.sfsync

/**
 * SF `IF_SendStaffReviewToPWS` 사원평가 마스터 조회 테스트 응답 (개발자 도구 — 외부 API 테스트).
 *
 * SF → PWS 방향 조회 결과를 운영자가 직접 검증하도록 SF 응답 원형을 그대로 노출한다.
 * 신규 DB 에는 저장하지 않는 순수 조회 테스트 결과 (클레임/거래처목표 마스터 조회 테스트와 동일 성격).
 *
 * @property success      SF 호출 성공 여부 (HTTP 200 + RESULT_CODE 정상).
 * @property resultCode   SF 응답의 `RESULT_CODE` (응답에 없으면 null).
 * @property resultMsg    SF 응답의 `RESULT_MSG` 또는 오류 요약 (응답에 없으면 null).
 * @property rawResponse  SF 응답 본문(raw JSON). 사원평가 마스터 목록이 이 안에 담겨 온다.
 * @property requestPayload SF 로 전송한 요청 body JSON (`{ "MOD_DT": "..." }`).
 */
data class AdminStaffReviewSyncTestResponse(
    val success: Boolean,
    val resultCode: String?,
    val resultMsg: String?,
    val rawResponse: String?,
    val requestPayload: String,
)
