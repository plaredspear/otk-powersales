package com.otoki.powersales.sap.inbound.dto.sales

import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

/**
 * SAP 인바운드 청크 처리 공용 DTO.
 *
 * 원래 `SalesHistoryDetail.kt` 에 함께 정의되어 있었으나, 일 매출 이력 SAP inbound 흐름이
 * ORORA DB 직접 적재로 대체되면서(Spec #855 Q5) `SalesHistoryDetail` 은 삭제되고, 여전히
 * 다른 SAP inbound 흐름(근태/발령 등)이 공유하는 [FailureItem] / [ChunkResult] 만 본 파일로 분리 보존한다.
 *
 * RESULT_DETAIL 내부 키는 SAP 호환 보존을 위해 SnakeCase 직렬화 (Spec #580 P1-B).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class FailureItem(
    val identifier: String?,
    val reason: String,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ChunkResult(
    val index: Int,
    val status: String,
    val count: Int,
    val reason: String? = null,
) {
    companion object {
        const val STATUS_SUCCESS: String = "success"
        const val STATUS_PARTIAL: String = "partial"
        const val STATUS_FAILED: String = "failed"
    }
}
