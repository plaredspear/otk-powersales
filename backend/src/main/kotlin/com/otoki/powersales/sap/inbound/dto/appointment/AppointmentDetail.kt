package com.otoki.powersales.sap.inbound.dto.appointment

import com.otoki.powersales.sap.inbound.dto.sales.FailureItem

/**
 * SAP 인사발령 인바운드 응답의 RESULT_DETAIL 페이로드. (Spec #562)
 *
 * 카운트 단위는 INSERT 1건 = 1. `failures.identifier` 는 `EmployeeCode + AppointDate`.
 */
data class AppointmentDetail(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<FailureItem>
)
