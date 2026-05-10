package com.otoki.powersales.sales.service.dto

/**
 * 월 매출 이력 UPSERT 실패 행.
 *
 * - [identifier] : 식별자 (UPSERT 키 또는 sapAccountCode 값)
 * - [reason] : 실패 사유
 */
data class MonthlySalesHistoryUpsertFailedRow(
    val identifier: String?,
    val reason: String
)
