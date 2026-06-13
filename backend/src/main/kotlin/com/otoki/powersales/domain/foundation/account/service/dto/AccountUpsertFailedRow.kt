package com.otoki.powersales.domain.foundation.account.service.dto

/**
 * 거래처 UPSERT 실패 행. 도메인 결과 [AccountUpsertResult.failures] 의 원소.
 *
 * - [identifier] : 식별자 (현재 채택은 `externalKey` 값 — 채널별로 의미가 다를 수 있어 도메인은 일반화된 식별자 1필드로 노출)
 * - [reason] : 실패 사유 (예: `"employee_code not found: 999999"`, `"ConsignmentAcc 형식 오류: y"`)
 */
data class AccountUpsertFailedRow(
    val identifier: String?,
    val reason: String
)
