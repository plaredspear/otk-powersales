package com.otoki.powersales.domain.sales.dto.response

import com.otoki.powersales.domain.sales.entity.DailySalesHistory
import java.time.LocalDateTime

/**
 * ORORA 일매출 목록 행 — `daily_sales_history` 1건 (SF `DailySalesHistory__c` 동등).
 *
 * 금액 3종은 ORORA 적재 배치(`OroraDailySalesChunkProcessor`)가 채우는 단일 금액 컬럼이다.
 * 온도대별 3분할 컬럼(`erp_sales_amount1~3` 등)은 SF 이관분에만 존재하고 ORORA 적재 경로는
 * 채우지 않아 화면에 노출하지 않는다.
 */
data class DailySalesHistoryListItem(
    val id: Long,
    /**
     * 매출발생일자 (`yyyyMMdd`).
     *
     * 레거시 정합 보정값 — 적재 대상월이 당월이면 적재일(today), 아니면 그 달 말일
     * (`OroraDailySalesChunkProcessor.resolveSalesDate`). ORORA 원본 일자는 [externalKey] 의 뒤 8자다.
     */
    val salesDate: String,
    val sapAccountCode: String,
    /** `거래처코드 + ORORA 원본 매출일자(yyyyMMdd)` — 적재 upsert 키. */
    val externalKey: String,
    /** 전산매출실적 (원). */
    val erpSalesAmount: Double?,
    /** 물류배부매출실적 (원). */
    val erpDistributionAmount: Double?,
    /** 원장매출 (원) — ORORA view 미제공이라 ORORA 적재분은 항상 null. */
    val ledgerAmount: Double?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(entity: DailySalesHistory): DailySalesHistoryListItem =
            DailySalesHistoryListItem(
                id = entity.id,
                salesDate = entity.salesDate,
                sapAccountCode = entity.sapAccountCode,
                externalKey = entity.externalKey,
                erpSalesAmount = entity.erpSalesAmount,
                erpDistributionAmount = entity.erpDistributionAmount,
                ledgerAmount = entity.ledgerAmount,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )
    }
}

/**
 * ORORA 일매출 목록 응답 — 거래처 1곳 + 매출월 1개의 일별 행 + 금액 합계.
 *
 * 합계 3종은 조회 결과 전량 기준이며, 같은 거래처+월의 `monthly_sales_history` 합계 컬럼
 * (`abcClosingSumAmount` / `shipClosingSumAmount` / `totalLedgerAmount`) 과 동일 집계다
 * (적재 배치가 daily 전량 재합산으로 월합계를 대입하므로 두 값이 어긋나면 적재 이상 신호).
 */
data class DailySalesHistoryListResponse(
    /** 조회 매출월 (`yyyyMM`). */
    val salesMonth: String,
    val sapAccountCode: String,
    val accountName: String?,
    val branchName: String?,
    val content: List<DailySalesHistoryListItem>,
    val totalErpSalesAmount: Double,
    val totalErpDistributionAmount: Double,
    val totalLedgerAmount: Double,
) {
    companion object {
        fun of(
            salesMonth: String,
            sapAccountCode: String,
            accountName: String?,
            branchName: String?,
            entities: List<DailySalesHistory>,
        ): DailySalesHistoryListResponse =
            DailySalesHistoryListResponse(
                salesMonth = salesMonth,
                sapAccountCode = sapAccountCode,
                accountName = accountName,
                branchName = branchName,
                content = entities.map { DailySalesHistoryListItem.from(it) },
                totalErpSalesAmount = entities.sumOf { it.erpSalesAmount ?: 0.0 },
                totalErpDistributionAmount = entities.sumOf { it.erpDistributionAmount ?: 0.0 },
                totalLedgerAmount = entities.sumOf { it.ledgerAmount ?: 0.0 },
            )
    }
}
