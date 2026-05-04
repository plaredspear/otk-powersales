package com.otoki.powersales.sap.inbound.dto.appointment

import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import com.otoki.powersales.sap.inbound.dto.sales.FailureItem

/**
 * SAP 인사발령 인바운드 응답의 RESULT_DETAIL 페이로드. (Spec #562)
 *
 * 카운트 단위는 INSERT 1건 = 1. `failures.identifier` 는 `EmployeeCode + AppointDate`.
 *
 * SAP 호환 보존을 위해 RESULT_DETAIL 내부 키는 SnakeCase 로 직렬화된다 (Spec #580 P1-B).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class AppointmentDetail(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<FailureItem>
)
