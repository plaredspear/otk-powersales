package com.otoki.powersales.sap.inbound.dto.account

/**
 * SAP 거래처 / 거래처 카테고리 마스터 인바운드 응답의 RESULT_DETAIL 페이로드. (Spec #558)
 *
 * 두 엔드포인트 모두 동일한 구조를 사용하지만 [FailureItem.identifier] 의 의미는 다르다.
 * - 거래처 (`POST /api/v1/sap/account`): identifier = SAPAccountCode (external_key)
 * - 카테고리 (`POST /api/v1/sap/account-category`): identifier = AccountCode
 */
data class AccountMasterDetail(
    val successCount: Int,
    val failureCount: Int,
    val failures: List<FailureItem>
)

data class FailureItem(
    val identifier: String?,
    val reason: String
)
