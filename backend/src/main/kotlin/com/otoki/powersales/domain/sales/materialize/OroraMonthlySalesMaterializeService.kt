package com.otoki.powersales.domain.sales.materialize

import com.otoki.powersales.domain.sales.enums.SalesMonth
import com.otoki.powersales.domain.sales.enums.SalesYear
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * ORORA 월별 매출 view → 메인 RDS `monthly_sales_history` 적재 서비스 (Spec #855).
 *
 * ## 레거시 매핑
 * 레거시 `IF_REST_ORORA_ReceiveMonthlySalesHistory.cls:39-145` 의 신규 동등 — SF 가 ORORA SAP PO REST
 * 어댑터(NS00030)로 outbound callout 하여 받던 월별 마감 데이터를, 신규는 ORORA MSSQL view
 * `ECRM_ABCCUST_MH_V` 직조회로 받아 RDS 에 upsert. read-through → materialize 전환.
 *
 * ## 동작 요약
 * 1. 입력: 대상 매출월(`YYYYMM`) + 거래처 코드 범위(`OroraAccountRange`, 레거시 `From_cust`/`To_cust` 동등).
 * 2. 분기: 거래처 범위를 chunk 로 분할 → 각 chunk 를 [OroraMonthlySalesChunkProcessor] (별도 트랜잭션) 위임.
 * 3. 외부 호출: ORORA MSSQL view 조회 (chunk processor 내부).
 * 4. 부수 효과: `external_key`(거래처+년+월) 기준 upsert — ABC/Ship 마감 + 합계 컬럼만 갱신.
 *    `this_month_target`/`remark`/`is_confirmed` 등 운영 입력 컬럼은 미변경 (레거시 bypass 동등, `cls:119-122`).
 *
 * ## 신규 차이
 * - SAP PO 어댑터 callout → ORORA DB 직조회 (legacy-deviation.md:108).
 * - `account_id` FK 매핑: SAPAccountCode → `account.external_key` 매칭. 미매칭은 null (적재 누락 방지,
 *   레거시 IFInsert 는 addError 였으나 신규는 보존).
 * - chunk 단위 트랜잭션 격리 (한 chunk 실패가 전체 롤백 유발 안 함).
 */
@Service
class OroraMonthlySalesMaterializeService(
    private val chunkProcessor: OroraMonthlySalesChunkProcessor,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 대상 매출월 + 거래처 범위의 ORORA 월별 마감을 `monthly_sales_history` 에 적재.
     *
     * 거래처 범위를 chunk 단위로 [OroraMonthlySalesChunkProcessor] 에 위임하여 부분 실패를 격리한다.
     */
    fun materialize(salesMonth: String, range: OroraAccountRange): OroraMonthlyMaterializeResult {
        require(salesMonth.length == 6) { "salesMonth 는 YYYYMM 6자: $salesMonth" }
        val salesYear = SalesYear.fromValueOrNull(salesMonth.substring(0, 4))
        val salesMonthEnum = SalesMonth.fromValueOrNull(salesMonth.substring(4, 6))
        require(salesYear != null && salesMonthEnum != null) { "salesMonth picklist 범위 밖: $salesMonth" }

        var fetched = 0
        var upserted = 0
        var unmatched = 0

        range.toChunks().forEachIndexed { idx, (fromCode, toCode) ->
            try {
                val result = chunkProcessor.process(salesMonth, salesYear, salesMonthEnum, fromCode, toCode)
                fetched += result.fetched
                upserted += result.upserted
                unmatched += result.unmatched
            } catch (ex: Exception) {
                log.warn(
                    "ORORA_MONTHLY_MATERIALIZE chunk {} 실패 (salesMonth={}, range={}~{}): {}",
                    idx, salesMonth, fromCode, toCode, ex.message,
                )
            }
        }

        log.info(
            "ORORA_MONTHLY_MATERIALIZE salesMonth={} fetched={} upserted={} unmatched={}",
            salesMonth, fetched, upserted, unmatched,
        )
        return OroraMonthlyMaterializeResult(salesMonth, fetched, upserted, unmatched)
    }
}
