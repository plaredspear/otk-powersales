package com.otoki.powersales.sap.inbound.dto.account

import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

/**
 * SAP 거래처 / 거래처 카테고리 마스터 인바운드 응답의 RESULT_DETAIL 페이로드. (Spec #558)
 *
 * 두 엔드포인트 모두 동일한 구조를 사용하지만 [FailureItem.identifier] 의 의미는 다르다.
 * - 거래처 (`POST /api/v1/sap/account`): identifier = SAPAccountCode (external_key)
 * - 카테고리 (`POST /api/v1/sap/account-category`): identifier = AccountCode
 *
 * SAP 호환 보존을 위해 RESULT_DETAIL 내부 키는 SnakeCase 로 직렬화된다 (Spec #580 P1-B).
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class AccountMasterDetail(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<FailureItem>
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class FailureItem(
    val identifier: String?,
    val reason: String
)
