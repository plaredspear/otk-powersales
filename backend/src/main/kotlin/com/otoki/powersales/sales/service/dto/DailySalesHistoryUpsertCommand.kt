package com.otoki.powersales.sales.service.dto

/**
 * 일 매출 이력 UPSERT 도메인 입력 커맨드.
 *
 * - UPSERT 키: `sapAccountCode + salesDate` 단순 연결 (= [com.otoki.powersales.sales.entity.DailySalesHistory.externalKey])
 *
 * 청크 분할은 어댑터 책임이며 도메인은 단일 청크 단위 입력만 다룬다.
 */
data class DailySalesHistoryUpsertCommand(
    val sapAccountCode: String?,
    val salesDate: String?,
    val erpSalesAmount1: String?,
    val erpSalesAmount2: String?,
    val erpSalesAmount3: String?,
    val erpDistributionAmount1: String?,
    val erpDistributionAmount2: String?,
    val erpDistributionAmount3: String?,
    val ledgerAmount: String?
)
