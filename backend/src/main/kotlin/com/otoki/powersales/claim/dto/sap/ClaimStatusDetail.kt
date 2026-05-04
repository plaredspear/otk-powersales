package com.otoki.powersales.claim.dto.sap

import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

/**
 * SAP 클레임 상태 인바운드 응답의 RESULT_DETAIL 페이로드. (Spec #561)
 *
 * `failures.identifier` 는 `name`.
 *
 * SAP 호환 보존을 위해 RESULT_DETAIL 내부 키는 SnakeCase 로 직렬화된다 (Spec #580 P1-B).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ClaimStatusDetail(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<ClaimStatusFailure>
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class ClaimStatusFailure(
    val name: String?,
    val reason: String
)
