package com.otoki.powersales.sap.inbound.dto.sales

import com.fasterxml.jackson.annotation.JsonIgnore
import com.otoki.powersales.sap.inbound.dto.SapInboundChunkedResult
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

/**
 * SAP 매출 이력 인바운드 응답의 RESULT_DETAIL 페이로드. (Spec #560)
 *
 * SAP 호환 보존을 위해 RESULT_DETAIL 내부 키는 SnakeCase 로 직렬화된다 (Spec #580 P1-B).
 *
 * Spec #639: [SapInboundChunkedResult] 구현 — `chunkCount` 는 `chunks.size` 에서 파생되며
 * 외부 응답 JSON 에는 포함되지 않는다 (`@get:JsonIgnore` — 본 필드는 [com.otoki.powersales.sap.auth.audit.SapInboundAuditAspect]
 * 의 audit reason 추출 전용).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SalesHistoryDetail(
    override val successCount: Int,
    override val failureCount: Int,
    val failures: List<FailureItem>,
    val chunks: List<ChunkResult>
) : SapInboundChunkedResult {
    @get:JsonIgnore
    override val chunkCount: Int get() = chunks.size
}

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
