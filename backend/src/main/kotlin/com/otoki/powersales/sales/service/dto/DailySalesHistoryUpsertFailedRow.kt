package com.otoki.powersales.sales.service.dto

/**
 * 일 매출 이력 UPSERT 실패 행.
 *
 * - [identifier] : 식별자 (UPSERT 키 또는 sapAccountCode 값)
 * - [reason] : 실패 사유 (예: `"SAPAccountCode 필수"`, `"SalesDate 형식 오류: 2026-04-27"`)
 */
data class DailySalesHistoryUpsertFailedRow(
    val identifier: String?,
    val reason: String
)
