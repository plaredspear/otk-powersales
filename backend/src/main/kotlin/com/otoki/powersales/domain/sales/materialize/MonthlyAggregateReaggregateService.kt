package com.otoki.powersales.domain.sales.materialize

import com.otoki.powersales.domain.sales.enums.SalesMonth
import com.otoki.powersales.domain.sales.enums.SalesYear
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * ORORA 조회 없이 이미 적재된 `daily_sales_history` 만으로 `monthly_sales_history` 합계를 재계산하는 서비스.
 *
 * ## 도입 배경 (사용자 결정 2026-07-09)
 * 월별 합계(abcClosingSum/shipClosingSum/totalLedger)는 일별 적재의 부수효과로 갱신되지만, 레거시
 * 트리거 비멱등 시절 어긋난 값이나 daily 를 손본 뒤 월합계를 다시 맞춰야 하는 상황이 있다. 이 경로는
 * ORORA 를 다시 조회하지 않고(=외부 연동 부담 0) 기존 daily 전량 SUM 으로 월합계만 정합한다.
 *
 * ## 동작
 * 1. 입력: 대상 매출월(`YYYYMM`) + 거래처 코드 범위(`OroraAccountRange`).
 * 2. 거래처 범위를 chunk 로 분할해 [OroraDailySalesChunkProcessor.reaggregateMonthly] (별도 트랜잭션) 위임.
 * 3. `daily_sales_history` 는 읽기만, `monthly_sales_history` 만 갱신. 운영 입력 컬럼(목표/비고/마감)은 보존.
 *
 * [OroraDailySalesMaterializeService] 와 chunk 순회 구조는 같으나, ORORA view 조회 / daily upsert 가 없다.
 */
@Service
class MonthlyAggregateReaggregateService(
    private val chunkProcessor: OroraDailySalesChunkProcessor,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 대상 매출월 + 거래처 범위의 월별 합계를 daily 전량 SUM 으로 재계산.
     *
     * 거래처 범위를 chunk 단위로 [OroraDailySalesChunkProcessor.reaggregateMonthly] 에 위임하여 부분 실패를 격리한다.
     */
    fun reaggregate(salesMonth: String, range: OroraAccountRange): MonthlyAggregateResult {
        require(salesMonth.length == 6) { "salesMonth 는 YYYYMM 6자: $salesMonth" }
        val salesYear = SalesYear.fromValueOrNull(salesMonth.substring(0, 4))
        val salesMonthEnum = SalesMonth.fromValueOrNull(salesMonth.substring(4, 6))
        require(salesYear != null && salesMonthEnum != null) { "salesMonth picklist 범위 밖: $salesMonth" }

        var monthlyUpdated = 0
        val chunks = range.toChunks()
        val totalChunks = chunks.size
        val startedNanos = System.nanoTime()
        chunks.forEachIndexed { idx, (fromCode, toCode) ->
            val chunkStartedNanos = System.nanoTime()
            // chunk 경계는 ORORA view 형식(선행 000)이나 daily_sales_history 는 prefix 제거 형식이라 벗겨서 넘긴다.
            val fromBare = fromCode.removePrefix(OroraAccountRange.ACCOUNT_CODE_PREFIX)
            val toBare = toCode.removePrefix(OroraAccountRange.ACCOUNT_CODE_PREFIX)
            try {
                val updated = chunkProcessor.reaggregateMonthly(salesMonth, salesYear, salesMonthEnum, fromBare, toBare)
                monthlyUpdated += updated
                log.info(
                    "MONTHLY_REAGGREGATE chunk {}/{} 완료 (range={}~{}) monthlyUpdated={} chunkMs={} cumulativeMs={}",
                    idx + 1, totalChunks, fromBare, toBare, updated,
                    (System.nanoTime() - chunkStartedNanos) / 1_000_000,
                    (System.nanoTime() - startedNanos) / 1_000_000,
                )
            } catch (ex: Exception) {
                log.warn(
                    "MONTHLY_REAGGREGATE chunk {}/{} 실패 (salesMonth={}, range={}~{}) chunkMs={}: {}",
                    idx + 1, totalChunks, salesMonth, fromBare, toBare,
                    (System.nanoTime() - chunkStartedNanos) / 1_000_000, ex.message,
                )
            }
        }

        log.info(
            "MONTHLY_REAGGREGATE salesMonth={} chunks={} monthlyUpdated={} elapsedMs={}",
            salesMonth, totalChunks, monthlyUpdated, (System.nanoTime() - startedNanos) / 1_000_000,
        )
        return MonthlyAggregateResult(salesMonth, monthlyUpdated)
    }
}
