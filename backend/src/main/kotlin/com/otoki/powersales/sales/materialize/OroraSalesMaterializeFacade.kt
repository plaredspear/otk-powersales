package com.otoki.powersales.sales.materialize

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ORORA 매출이력 적재의 진입 facade (Spec #855).
 *
 * 배치 / 수동 트리거 공통으로 사용하는 거래처 범위 조립 + 대상 월 결정(동적 산출, Q2) 을 한 곳에 둔다.
 * 거래처 범위 기본값은 레거시 `OroraDailyAccountRange__mdt` 실측(from 1000000 / to 1100000 / range 2000) 정합.
 */
@Service
class OroraSalesMaterializeFacade(
    private val monthlyService: OroraMonthlySalesMaterializeService,
    private val dailyService: OroraDailySalesMaterializeService,
    @Value("\${app.batch.orora.account-range.from:1000000}") private val rangeFrom: Long,
    @Value("\${app.batch.orora.account-range.to:1100000}") private val rangeTo: Long,
    @Value("\${app.batch.orora.account-range.chunk-size:2000}") private val chunkSize: Long,
) {

    /**
     * 월별 적재 — `salesMonth` null 이면 당월 동적 산출 (Q2 옵션 1).
     */
    fun materializeMonthly(salesMonth: String? = null): OroraMonthlyMaterializeResult =
        monthlyService.materialize(resolveSalesMonth(salesMonth), accountRange())

    /**
     * 일별 적재 + 월별 합계 갱신 — `salesMonth` null 이면 당월 동적 산출 (Q2 옵션 1).
     */
    fun materializeDaily(salesMonth: String? = null): OroraDailyMaterializeResult =
        dailyService.materialize(resolveSalesMonth(salesMonth), accountRange())

    private fun accountRange(): OroraAccountRange = OroraAccountRange(rangeFrom, rangeTo, chunkSize)

    /**
     * 대상 매출월 결정 — 명시값 우선, 없으면 현재 시각 기준 당월 `YYYYMM` (레거시 mdt 수동 갱신 → 동적 산출).
     */
    private fun resolveSalesMonth(salesMonth: String?): String =
        salesMonth?.takeIf { it.isNotBlank() } ?: LocalDate.now().format(YYYYMM)

    companion object {
        private val YYYYMM: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMM")
    }
}
