package com.otoki.powersales.sap.inbound.dto.sales

/**
 * SAP 매출 이력 인바운드 응답의 RESULT_DETAIL 페이로드. (Spec #560)
 */
data class SalesHistoryDetail(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<FailureItem>,
    val chunks: List<ChunkResult>
)

data class FailureItem(
    val identifier: String?,
    val reason: String
)

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
