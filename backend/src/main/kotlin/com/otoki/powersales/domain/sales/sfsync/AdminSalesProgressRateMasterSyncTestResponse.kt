package com.otoki.powersales.domain.sales.sfsync

/**
 * SF `IF_salesprogresssend` 거래처목표등록마스터 조회 테스트 응답 (개발자 도구 — 외부 API 테스트).
 *
 * SF → PWS 방향 조회 결과를 운영자가 직접 검증하도록 SF 응답 원형을 그대로 노출한다.
 * 요청이 `save=true` 였으면 주기 sync 와 동일 경로(ExternalKey upsert)로 신규 DB 에 저장한 통계도 담는다.
 *
 * @property success      SF 호출 성공 여부 (HTTP 200 + RESULT_CODE 정상).
 * @property resultCode   SF 응답의 `RESULT_CODE` (응답에 없으면 null).
 * @property resultMsg    SF 응답의 `RESULT_MSG` 또는 오류 요약 (응답에 없으면 null).
 * @property rawResponse  SF 응답 본문(raw JSON). 거래처목표등록마스터 목록이 이 안에 담겨 온다.
 * @property requestPayload SF 로 전송한 요청 body JSON (`{ "MOD_DT": "..." }`).
 * @property syncResult   DB 저장(upsert) 통계. 조회 전용 요청(`save=false`) 또는 SF 호출 실패 시 null.
 */
data class AdminSalesProgressRateMasterSyncTestResponse(
    val success: Boolean,
    val resultCode: String?,
    val resultMsg: String?,
    val rawResponse: String?,
    val requestPayload: String,
    val syncResult: SyncSummary? = null,
) {

    /**
     * DB 저장(upsert) 통계 — [SalesProgressRateMasterSyncService.SyncResult] 의 API 노출용 사본.
     *
     * @property fetched  SF 응답에서 파싱된 레코드 수.
     * @property inserted 신규 INSERT 건수.
     * @property updated  기존 row UPDATE 건수.
     * @property skipped  ExternalKey 산출 불가로 skip 된 건수.
     */
    data class SyncSummary(
        val fetched: Int,
        val inserted: Int,
        val updated: Int,
        val skipped: Int,
    )
}
