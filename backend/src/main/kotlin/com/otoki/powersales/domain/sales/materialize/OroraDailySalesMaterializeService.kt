package com.otoki.powersales.domain.sales.materialize

import com.otoki.powersales.domain.sales.enums.SalesMonth
import com.otoki.powersales.domain.sales.enums.SalesYear
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * ORORA 일별 매출 view → 메인 RDS `daily_sales_history` 적재 + 월별 합계 갱신 서비스 (Spec #855).
 *
 * ## 레거시 매핑
 * 레거시 `Queueable_OroraDailySalesHistory_M1.cls:51-185` (ORORA pull) + `DailyErpSalesInfoTriggerHandler`
 * (일별→월별 합계 자동 갱신 트리거 부수효과) 의 신규 동등. 두 채널(ORORA + SAP inbound) 모두 SObject upsert 가
 * 트리거를 발동시켜 월별 합계를 갱신했으나, 신규는 SAP inbound 를 삭제(Spec #855 Q5)하고 ORORA 적재 서비스가
 * 트리거 부수효과를 명시 인계한다.
 *
 * ## 동작 요약
 * 1. 입력: 대상 매출월(`YYYYMM`) + 거래처 코드 범위(`OroraAccountRange`).
 * 2. 분기: 거래처 범위 chunk 별로 [OroraDailySalesChunkProcessor] (별도 트랜잭션) 위임 — 일별 upsert + 월별 합계 갱신.
 * 3. 외부 호출: ORORA MSSQL view 조회 (chunk processor 내부).
 * 4. 부수 효과: `daily_sales_history` upsert (단일 금액 2컬럼) + 해당 월 `monthly_sales_history.total_ledger_amount` 갱신.
 *
 * ## 레거시 raw 동작 정합 (사용자 결정 2026-06-22)
 * - SalesDate: 레거시 보정 동작 복원 — 대상월==today연월이면 `today`, 아니면 그 달 말일 (Queueable:113-119).
 *   external key 는 원본 `YYYYMMDD` 유지 (key 와 저장 SalesDate 가 어긋나는 레거시 동작 포함).
 * - 월별 갱신: `abcClosingSumAmount`(ΣERPSales) + `shipClosingSumAmount`(ΣERPDist) + `totalLedgerAmount`(ΣLedger=0).
 *   insert 경로=합산 / update 경로=마지막 1건 값 덮어쓰기 (레거시 트리거 raw 동작, update buggy 경로 포함).
 * - 거래처 미매칭 row 는 적재하지 않음 (레거시 TriggerHandler addError 동등).
 */
@Service
class OroraDailySalesMaterializeService(
    private val chunkProcessor: OroraDailySalesChunkProcessor,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 대상 매출월 + 거래처 범위의 ORORA 일별 매출을 적재하고 월별 합계를 갱신.
     *
     * 거래처 범위를 chunk 단위로 [OroraDailySalesChunkProcessor] 에 위임하여 부분 실패를 격리한다.
     */
    fun materialize(salesMonth: String, range: OroraAccountRange): OroraDailyMaterializeResult {
        require(salesMonth.length == 6) { "salesMonth 는 YYYYMM 6자: $salesMonth" }
        val salesYear = SalesYear.fromValueOrNull(salesMonth.substring(0, 4))
        val salesMonthEnum = SalesMonth.fromValueOrNull(salesMonth.substring(4, 6))
        require(salesYear != null && salesMonthEnum != null) { "salesMonth picklist 범위 밖: $salesMonth" }

        var dailyUpserted = 0
        var monthlyUpdated = 0

        range.toChunks().forEachIndexed { idx, (fromCode, toCode) ->
            try {
                val result = chunkProcessor.process(salesMonth, salesYear, salesMonthEnum, fromCode, toCode)
                dailyUpserted += result.dailyUpserted
                monthlyUpdated += result.monthlyUpdated
            } catch (ex: Exception) {
                log.warn(
                    "ORORA_DAILY_MATERIALIZE chunk {} 실패 (salesMonth={}, range={}~{}): {}",
                    idx, salesMonth, fromCode, toCode, ex.message,
                )
            }
        }

        log.info(
            "ORORA_DAILY_MATERIALIZE salesMonth={} dailyUpserted={} monthlyUpdated={}",
            salesMonth, dailyUpserted, monthlyUpdated,
        )
        return OroraDailyMaterializeResult(salesMonth, dailyUpserted, monthlyUpdated)
    }
}
