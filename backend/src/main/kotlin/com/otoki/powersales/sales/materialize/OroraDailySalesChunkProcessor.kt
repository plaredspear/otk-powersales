package com.otoki.powersales.sales.materialize

import com.otoki.orora.entity.OroraDailySalesHistory
import com.otoki.orora.repository.OroraDailySalesHistoryRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.sales.entity.DailySalesHistory
import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.sales.enums.SalesMonth
import com.otoki.powersales.sales.enums.SalesYear
import com.otoki.powersales.sales.repository.DailySalesHistoryRepository
import com.otoki.powersales.sales.repository.MonthlySalesHistoryRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * ORORA 일별 매출 적재 + 월별 합계 갱신의 단일 chunk 처리 (Spec #855).
 *
 * [OroraDailySalesMaterializeService] 의 루프에서 chunk 단위로 호출되며, 별도 빈으로 분리하여
 * `REQUIRES_NEW` 트랜잭션 프록시가 정상 적용되도록 한다. 한 chunk 의 일별 upsert + 월별 합계 갱신을
 * 하나의 트랜잭션으로 묶어 원자성을 보장한다 (레거시 SObject upsert + trigger 부수효과 동등, §2.3).
 */
@Component
class OroraDailySalesChunkProcessor(
    private val ororaDailyRepository: OroraDailySalesHistoryRepository,
    private val dailySalesHistoryRepository: DailySalesHistoryRepository,
    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository,
    private val accountRepository: AccountRepository,
) {

    data class ChunkResult(val dailyUpserted: Int, val monthlyUpdated: Int)

    /**
     * 단일 chunk 의 일별 적재 + 월별 합계 갱신을 하나의 트랜잭션으로 처리.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun process(
        salesMonth: String,
        salesYear: SalesYear,
        salesMonthEnum: SalesMonth,
        fromCode: String,
        toCode: String,
    ): ChunkResult {
        val ororaRows = ororaDailyRepository
            .findBySalesDateStartingWithAndSapAccountCodeBetween(salesMonth, fromCode, toCode)
        if (ororaRows.isEmpty()) return ChunkResult(0, 0)

        val sapCodes = ororaRows.map { it.sapAccountCode.removePrefix(OroraAccountRange.ACCOUNT_CODE_PREFIX) }.distinct()
        val accountByCode = accountRepository.findByExternalKeyIn(sapCodes).associateBy { it.externalKey }

        val dailyUpserted = upsertDaily(ororaRows, accountByCode)
        val monthlyUpdated = updateMonthlyAggregate(salesYear, salesMonthEnum, sapCodes, accountByCode)
        return ChunkResult(dailyUpserted, monthlyUpdated)
    }

    /**
     * ORORA 일별 row 를 `daily_sales_history` 에 `external_key` 기준 upsert (단일 금액 2컬럼만).
     * 온도대별(1~3)/`ledger_amount` 는 ORORA view 미제공이라 미적재 (§2.4).
     */
    private fun upsertDaily(
        ororaRows: List<OroraDailySalesHistory>,
        accountByCode: Map<String?, Account>,
    ): Int {
        val externalKeys = ororaRows.map { externalKey(it) }
        val existingByKey = dailySalesHistoryRepository.findByExternalKeyIn(externalKeys.distinct())
            .associateBy { it.externalKey }
            .toMutableMap()

        val toSave = mutableListOf<DailySalesHistory>()
        ororaRows.forEach { orora ->
            val sapCode = orora.sapAccountCode.removePrefix(OroraAccountRange.ACCOUNT_CODE_PREFIX)
            val key = sapCode + orora.salesDate
            val account = accountByCode[sapCode]

            val entity = existingByKey[key] ?: DailySalesHistory(
                sapAccountCode = sapCode,
                salesDate = orora.salesDate,
                externalKey = key,
            ).also { existingByKey[key] = it }

            entity.erpSalesAmount = orora.erpSalesAmount?.toDouble()
            entity.erpDistributionAmount = orora.erpDistributionAmount?.toDouble()
            if (account != null) {
                entity.account = account
                entity.accountSfid = account.sfid
            }
            toSave += entity
        }
        dailySalesHistoryRepository.saveAll(toSave)
        return toSave.size
    }

    /**
     * 일별 적재 직후 해당 월의 `monthly_sales_history.total_ledger_amount` 를 daily 합산으로 갱신
     * (레거시 트리거 파생, §2.3). Q4 옵션 1: ABC/Ship 마감 합계는 월별 적재(§2.1)가 정본이므로
     * 본 경로는 건드리지 않고 원장 합계만 daily `erp_sales + erp_distribution` 누적으로 갱신한다.
     */
    private fun updateMonthlyAggregate(
        salesYear: SalesYear,
        salesMonthEnum: SalesMonth,
        sapCodes: List<String>,
        accountByCode: Map<String?, Account>,
    ): Int {
        val yyyymm = salesYear.value + salesMonthEnum.value
        val existing = monthlySalesHistoryRepository
            .findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(listOf(salesYear), listOf(salesMonthEnum), sapCodes)
            .filter { it.externalkeyC != null }
            .associateBy { it.sapAccountCode }

        val toSave = mutableListOf<MonthlySalesHistory>()
        sapCodes.forEach { sapCode ->
            val ledgerSum = ledgerSumForAccountMonth(sapCode, yyyymm)
            val key = sapCode + salesYear.value + salesMonthEnum.value
            val entity = existing[sapCode] ?: MonthlySalesHistory(
                sapAccountCode = sapCode,
                salesYear = salesYear,
                salesMonth = salesMonthEnum,
                externalkeyC = key,
            ).also {
                accountByCode[sapCode]?.let { acc -> it.account = acc; it.accountSfid = acc.sfid }
            }
            entity.totalLedgerAmount = ledgerSum
            toSave += entity
        }
        monthlySalesHistoryRepository.saveAll(toSave)
        return toSave.size
    }

    /**
     * 거래처 + 매출월(`YYYYMM`) 의 daily 단일 금액 합 (erp_sales + erp_distribution) → 월 원장 합계 대용.
     */
    private fun ledgerSumForAccountMonth(sapCode: String, yyyymm: String): BigDecimal {
        val rows = dailySalesHistoryRepository.findBySapAccountCodeAndSalesDateStartingWith(sapCode, yyyymm)
        return rows.fold(BigDecimal.ZERO) { acc, r ->
            acc + BigDecimal.valueOf((r.erpSalesAmount ?: 0.0) + (r.erpDistributionAmount ?: 0.0))
        }
    }

    private fun externalKey(orora: OroraDailySalesHistory): String =
        orora.sapAccountCode.removePrefix(OroraAccountRange.ACCOUNT_CODE_PREFIX) + orora.salesDate
}
