package com.otoki.powersales.admin.dto.request

/**
 * ORORA 월매출 수동 적재 트리거 요청.
 *
 * @property salesMonth 적재 대상 매출월 (`YYYYMM` 6자, 예: `202604`). null/blank 면 전월 자동 산출.
 */
data class OroraMonthlyMaterializeTriggerRequest(
    val salesMonth: String? = null,
)
