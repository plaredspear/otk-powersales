package com.otoki.powersales.domain.activity.order.service.dto

/**
 * ERP 주문 UPSERT 실패 행 (헤더 단위).
 *
 * - [identifier] : 식별자 (현재 채택은 sapOrderNumber 값)
 * - [reason] : 실패 사유 (예: `"SAPOrderNumber 필수"`, `"account not found"`)
 */
data class ErpOrderUpsertFailedRow(
    val identifier: String?,
    val reason: String
)
