package com.otoki.powersales.sales.service.dto

/**
 * 월 매출 이력 UPSERT 도메인 입력 커맨드.
 *
 * - UPSERT 키: `sapAccountCode + salesYearMonth` 단순 연결 (= [com.otoki.powersales.sales.entity.MonthlySalesHistory.externalkeyC])
 *
 * 청크 분할은 어댑터 책임이며 도메인은 단일 청크 단위 입력만 다룬다.
 */
data class MonthlySalesHistoryUpsertCommand(
    val sapAccountCode: String?,
    val salesYearMonth: String?,
    val abcClosingAmount1: String?,
    val abcClosingAmount2: String?,
    val abcClosingAmount3: String?,
    val totalLedgerAmount: String?,
    val shipClosingAmount: String?,
    val rlsales: String?
)
