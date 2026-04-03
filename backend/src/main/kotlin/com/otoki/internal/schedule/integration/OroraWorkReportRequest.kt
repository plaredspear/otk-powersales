package com.otoki.internal.schedule.integration

/**
 * Orora API 출근보고 요청 데이터 (안전점검 데이터 포함)
 */
data class OroraWorkReportRequest(
    val scheduleId: Long,
    val reason: String? = null,
    val equipment1: String? = null,
    val equipment2: String? = null,
    val equipment3: String? = null,
    val equipment4: String? = null,
    val equipment5: String? = null,
    val equipment6: String? = null,
    val equipment7: String? = null,
    val equipment8: String? = null,
    val equipment9: String? = null,
    val yesCount: Int? = null,
    val noCount: Int? = null,
    val startTime: String? = null,
    val completeTime: String? = null,
    val precaution: String? = null,
    val precautionCount: Int? = null,
    val traversalFlag: String? = null
)
