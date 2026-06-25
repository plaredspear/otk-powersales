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
