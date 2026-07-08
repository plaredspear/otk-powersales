package com.otoki.powersales.domain.sales.materialize

/**
 * ORORA 월별 매출 적재 결과 (Spec #855).
 *
 * @property salesMonth 대상 매출월 (`YYYYMM`)
 * @property fetchedCount ORORA view 에서 조회된 row 수
 * @property upsertedCount RDS 에 적재(신규+갱신)된 row 수
 * @property skippedAccountUnmatchedCount account 미매칭으로 account_id=null 적재된 row 수
 */
data class OroraMonthlyMaterializeResult(
    val salesMonth: String,
    val fetchedCount: Int,
    val upsertedCount: Int,
    val skippedAccountUnmatchedCount: Int,
)

/**
 * ORORA 일별 매출 적재 결과 (Spec #855).
 *
 * @property salesMonth 대상 매출월 (`YYYYMM`)
 * @property dailyUpsertedCount `daily_sales_history` 에 적재된 row 수
 * @property monthlyAggregateUpdatedCount 일별 합계로 갱신/생성된 `monthly_sales_history` row 수
 */
data class OroraDailyMaterializeResult(
    val salesMonth: String,
    val dailyUpsertedCount: Int,
    val monthlyAggregateUpdatedCount: Int,
)

/**
 * 월별 합계 재집계 결과 — ORORA 조회 없이 `daily_sales_history` 만으로 `monthly_sales_history` 재계산.
 *
 * @property salesMonth 대상 매출월 (`YYYYMM`)
 * @property monthlyAggregateUpdatedCount 재집계로 갱신/생성된 `monthly_sales_history` row 수
 */
data class MonthlyAggregateResult(
    val salesMonth: String,
    val monthlyAggregateUpdatedCount: Int,
)
