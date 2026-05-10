package com.otoki.powersales.account.service.dto

/**
 * 거래처 카테고리 UPSERT 실패 행. 도메인 결과 [AccountCategoryUpsertResult.failures] 의 원소.
 *
 * - [identifier] : 식별자 (현재 채택은 `accountCode` 값)
 * - [reason] : 실패 사유 (예: `"AccountCode 필수"`, `"Name 필수"`)
 */
data class AccountCategoryUpsertFailedRow(
    val identifier: String?,
    val reason: String
)
