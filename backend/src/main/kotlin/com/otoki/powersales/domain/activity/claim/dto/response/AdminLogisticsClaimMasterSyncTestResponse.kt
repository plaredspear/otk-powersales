package com.otoki.powersales.domain.activity.claim.dto.response

/**
 * SF `IF_SendLogisticsClaimToPWS` 물류 클레임 마스터 조회 테스트 응답 (개발자 도구 — 외부 API 테스트).
 *
 * SF → PWS 방향의 물류 클레임(제안) 마스터 조회 결과를 운영자가 직접 검증하도록 SF 응답 원형을 그대로 노출한다.
 * 신규 DB 에는 저장하지 않는 순수 조회 테스트 결과.
 *
 * @property success      SF 호출 성공 여부 (HTTP 200 + RESULT_CODE 정상).
 * @property resultCode   SF 응답의 `RESULT_CODE` (응답에 없으면 null).
 * @property resultMsg    SF 응답의 `RESULT_MSG` 또는 오류 요약 (응답에 없으면 null).
 * @property rawResponse  SF 응답 본문(raw JSON). 물류 클레임 마스터 목록이 이 안에 담겨 온다.
 * @property requestPayload SF 로 전송한 요청 body JSON (`{ "MOD_DT": "..." }`).
 */
data class AdminLogisticsClaimMasterSyncTestResponse(
    val success: Boolean,
    val resultCode: String?,
    val resultMsg: String?,
    val rawResponse: String?,
    val requestPayload: String,
)
