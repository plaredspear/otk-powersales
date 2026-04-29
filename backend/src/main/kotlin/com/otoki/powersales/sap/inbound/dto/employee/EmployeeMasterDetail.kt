package com.otoki.powersales.sap.inbound.dto.employee

/**
 * SAP 직원 마스터 인바운드 응답의 RESULT_DETAIL 페이로드. (Spec #557)
 */
data class EmployeeMasterDetail(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<FailureItem>
)

data class FailureItem(
    val empCode: String?,
    val reason: String
)
