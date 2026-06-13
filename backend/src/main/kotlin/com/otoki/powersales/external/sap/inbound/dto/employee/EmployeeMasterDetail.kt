package com.otoki.powersales.external.sap.inbound.dto.employee

import com.otoki.powersales.external.sap.inbound.dto.SapInboundUpsertResult
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

/**
 * SAP 직원 마스터 인바운드 응답의 RESULT_DETAIL 페이로드. (Spec #557)
 *
 * SAP 호환 보존을 위해 RESULT_DETAIL 내부 키는 SnakeCase 로 직렬화된다 (Spec #580 P1-B).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class EmployeeMasterDetail(
    override val successCount: Int,
    override val failureCount: Int,
    val failures: List<FailureItem>
) : SapInboundUpsertResult

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class FailureItem(
    val empCode: String?,
    val reason: String
)
