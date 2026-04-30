package com.otoki.powersales.claim.dto.sap

/**
 * SAP 클레임 상태 인바운드 응답의 RESULT_DETAIL 페이로드. (Spec #561)
 *
 * `failures.identifier` 는 `name`.
 */
data class ClaimStatusDetail(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<ClaimStatusFailure>
)

data class ClaimStatusFailure(
    val name: String?,
    val reason: String
)
