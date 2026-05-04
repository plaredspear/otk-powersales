package com.otoki.powersales.sap.inbound.dto.sales

import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

/**
 * SAP 매출 이력 인바운드 응답의 RESULT_DETAIL 페이로드. (Spec #560)
 *
 * SAP 호환 보존을 위해 RESULT_DETAIL 내부 키는 SnakeCase 로 직렬화된다 (Spec #580 P1-B).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SalesHistoryDetail(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<FailureItem>,
    val chunks: List<ChunkResult>
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class FailureItem(
    val identifier: String?,
    val reason: String
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ChunkResult(
    val index: Int,
    val status: String,
    val count: Int,
    val reason: String? = null
) {
    companion object {
        const val STATUS_SUCCESS: String = "success"
        const val STATUS_PARTIAL: String = "partial"
        const val STATUS_FAILED: String = "failed"
    }
}
