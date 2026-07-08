package com.otoki.powersales.admin.dto.request

/**
 * ORORA 월매출 수동 적재 트리거 요청.
 *
 * @property salesMonth 적재 대상 매출월 (`YYYYMM` 6자, 예: `202604`). null/blank 면 전월 자동 산출.
 */
data class OroraMonthlyMaterializeTriggerRequest(
    val salesMonth: String? = null,
)

/**
 * ORORA 월매출 거래처 청크 단위 수동 적재 트리거 요청.
 *
 * 전체 거래처 범위를 도는 [OroraMonthlyMaterializeTriggerRequest] 와 달리, `chunkIndex`(0-based) 로
 * 지정한 단일 거래처 청크만 적재한다.
 *
 * @property chunkIndex 적재 대상 청크 번호 (0-based). `[0, 전체 청크 수)` 범위.
 * @property salesMonth 적재 대상 매출월 (`YYYYMM` 6자, 예: `202604`). null/blank 면 전월 자동 산출.
 */
data class OroraMonthlyMaterializeChunkTriggerRequest(
    val chunkIndex: Int,
    val salesMonth: String? = null,
)

/**
 * ORORA 일매출 수동 적재 트리거 요청.
 *
 * @property salesMonth 적재 대상 매출월 (`YYYYMM` 6자, 예: `202606`). null/blank 면 당월 자동 산출.
 */
data class OroraDailyMaterializeTriggerRequest(
    val salesMonth: String? = null,
)

/**
 * ORORA 일매출 거래처 청크 단위 수동 적재 트리거 요청.
 *
 * 전체 거래처 범위를 도는 [OroraDailyMaterializeTriggerRequest] 와 달리, `chunkIndex`(0-based) 로
 * 지정한 단일 거래처 청크만 적재한다.
 *
 * @property chunkIndex 적재 대상 청크 번호 (0-based). `[0, 전체 청크 수)` 범위.
 * @property salesMonth 적재 대상 매출월 (`YYYYMM` 6자, 예: `202606`). null/blank 면 당월 자동 산출.
 */
data class OroraDailyMaterializeChunkTriggerRequest(
    val chunkIndex: Int,
    val salesMonth: String? = null,
)

/**
 * 월별 합계 재집계 수동 트리거 요청 — ORORA 조회 없이 `daily_sales_history` 로 `monthly_sales_history` 재계산.
 *
 * @property salesMonth 재집계 대상 매출월 (`YYYYMM` 6자). **필수** — 재집계는 자동 산출하지 않는다.
 */
data class MonthlyReaggregateTriggerRequest(
    val salesMonth: String? = null,
)

/**
 * 월별 합계 재집계 거래처 청크 단위 수동 트리거 요청.
 *
 * @property chunkIndex 대상 청크 번호 (0-based). `[0, 전체 청크 수)` 범위.
 * @property salesMonth 재집계 대상 매출월 (`YYYYMM` 6자). **필수**.
 */
data class MonthlyReaggregateChunkTriggerRequest(
    val chunkIndex: Int,
    val salesMonth: String? = null,
)
