package com.otoki.powersales.domain.sales.sfsync

/**
 * SF 거래처목표등록마스터 동기화 수동 실행 테스트 응답 (개발자 도구 — 외부 API 테스트).
 *
 * 주기 배치([com.otoki.powersales.platform.batch.SalesProgressRateMasterSyncBatch]) 와 동일하게
 * SF fetch → ExternalKey 기준 upsert 를 즉시 1회 실행한 결과 통계.
 *  - [fetched] : SF 에서 가져온 row 수.
 *  - [inserted] / [updated] : INSERT / UPDATE 된 row 수.
 *  - [skipped] : ExternalKey 산출 불가로 건너뛴 row 수.
 *
 * SF fetch 통신부가 아직 미구현(TODO)이라 현재는 fetched=0 의 no-op 으로 동작한다.
 */
data class AdminSalesProgressRateMasterSyncTestResponse(
    val fetched: Int,
    val inserted: Int,
    val updated: Int,
    val skipped: Int,
) {
    companion object {
        fun from(result: SalesProgressRateMasterSyncService.SyncResult) =
            AdminSalesProgressRateMasterSyncTestResponse(
                fetched = result.fetched,
                inserted = result.inserted,
                updated = result.updated,
                skipped = result.skipped,
            )
    }
}
