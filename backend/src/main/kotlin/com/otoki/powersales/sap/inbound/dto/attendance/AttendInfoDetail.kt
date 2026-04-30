package com.otoki.powersales.sap.inbound.dto.attendance

import com.otoki.powersales.sap.inbound.dto.sales.ChunkResult
import com.otoki.powersales.sap.inbound.dto.sales.FailureItem

/**
 * SAP 출근 정보 인바운드 응답의 RESULT_DETAIL 페이로드. (Spec #562)
 *
 * 청크 단위 처리이므로 `chunks` 결과를 함께 보고한다 (#560 패턴 동일).
 */
data class AttendInfoDetail(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<FailureItem>,
    val chunks: List<ChunkResult>
)
