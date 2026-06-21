package com.otoki.powersales.domain.sales.materialize

import com.otoki.orora.entity.OroraDailySalesHistory
import com.otoki.orora.repository.OroraDailySalesHistoryRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.sales.entity.DailySalesHistory
import com.otoki.powersales.domain.sales.entity.MonthlySalesHistory
import com.otoki.powersales.domain.sales.enums.SalesMonth
import com.otoki.powersales.domain.sales.enums.SalesYear
import com.otoki.powersales.domain.sales.repository.DailySalesHistoryRepository
import com.otoki.powersales.domain.sales.repository.MonthlySalesHistoryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * ORORA 일별 매출 적재 + 월별 합계 갱신의 단일 chunk 처리 (Spec #855).
 *
 * [OroraDailySalesMaterializeService] 의 루프에서 chunk 단위로 호출되며, 별도 빈으로 분리하여
 * `REQUIRES_NEW` 트랜잭션 프록시가 정상 적용되도록 한다. 한 chunk 의 일별 upsert + 월별 합계 갱신을
 * 하나의 트랜잭션으로 묶어 원자성을 보장한다 (레거시 SObject upsert + trigger 부수효과 동등, §2.3).
 *
 * ## 레거시 동작 정합 (Queueable_OroraDailySalesHistory_M1 + DailyErpSalesInfoTriggerHandler)
 * 레거시 트리거의 raw 동작을 그대로 재현한다:
 * - 일별 SalesDate 는 대상월==today연월이면 `today`, 아니면 그 달 말일로 보정 (Queueable:113-119).
 * - 거래처(Account) 미매칭 row 는 일별 적재 자체를 차단 (TriggerHandler:79-80 `addError` 동등).
 * - 월별 갱신 컬럼은 `abcClosingSumAmount`(ΣERPSales) + `shipClosingSumAmount`(ΣERPDist) + `totalLedgerAmount`(ΣLedger).
 *   온도대별 마감 컬럼 / 목표 / 비고 / 마감 boolean 은 보존.
 * - insert 경로: 거래처+월 신규 일별 row 들의 **합산** (TriggerHandler beforeInsert :99-108).
 * - update 경로: 거래처+월 기존 일별 row 들의 **마지막 1건 값** (TriggerHandler beforeUpdate :257-259, `=` 단일 대입).
 * - 일별 LedgerAmount 는 ORORA view 미제공이라 항상 null → TotalLedger = Σnull = 0 (레거시 실측 동등).
 */
@Component
class OroraDailySalesChunkProcessor(
    private val ororaDailyRepository: OroraDailySalesHistoryRepository,
    private val dailySalesHistoryRepository: DailySalesHistoryRepository,
    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository,
    private val accountRepository: AccountRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    data class ChunkResult(val dailyUpserted: Int, val monthlyUpdated: Int)

    /** 일별 적재 결과 1건 — 월별 합계 경로 분기(insert/update)에 필요한 메타를 함께 보관. */
    private data class DailyUpsertOutcome(
        val entity: DailySalesHistory,
        val sapCode: String,
        val wasNew: Boolean,
    )

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

        val outcomes = upsertDaily(salesMonth, ororaRows, accountByCode)
        val monthlyUpdated = updateMonthlyAggregate(salesYear, salesMonthEnum, outcomes, accountByCode)
        return ChunkResult(outcomes.size, monthlyUpdated)
    }

    /**
     * ORORA 일별 row 를 `daily_sales_history` 에 `external_key` 기준 upsert (단일 금액 2컬럼만).
     * 온도대별(1~3)/`ledger_amount` 는 ORORA view 미제공이라 미적재 (§2.4).
     *
     * 레거시 정합:
     * - SalesDate 보정 (Queueable:113-119): 대상월==today연월 → today, 아니면 그 달 말일.
     * - 거래처 미매칭 row 차단 (TriggerHandler:79-80): Account 가 없으면 적재하지 않고 skip + 로그.
     */
    private fun upsertDaily(
        salesMonth: String,
        ororaRows: List<OroraDailySalesHistory>,
        accountByCode: Map<String?, Account>,
    ): List<DailyUpsertOutcome> {
        // 레거시 SalesDate__c 저장값은 보정(today/말일), external key 는 원본 YYYYMMDD (Queueable:115-145).
        // 둘을 분리한다: salesDate 컬럼=보정값, external_key=거래처코드+원본일자.
        val salesDateColumn = resolveSalesDate(salesMonth).token

        val externalKeys = ororaRows.map { externalKey(it) }
        val existingByKey = dailySalesHistoryRepository.findByExternalKeyIn(externalKeys.distinct())
            .associateBy { it.externalKey }
            .toMutableMap()

        val outcomes = mutableListOf<DailyUpsertOutcome>()
        val toSave = mutableListOf<DailySalesHistory>()
        ororaRows.forEach { orora ->
            val sapCode = orora.sapAccountCode.removePrefix(OroraAccountRange.ACCOUNT_CODE_PREFIX)
            val account = accountByCode[sapCode]
            // 레거시 TriggerHandler:79-80: Account 미매칭 row 는 일별·월별 모두 생성하지 않는다.
            if (account == null) {
                log.warn("ORORA_DAILY_MATERIALIZE SAPAccountCode [{}] is not found — row skip", sapCode)
                return@forEach
            }

            val key = sapCode + orora.salesDate    // 레거시 external key = 거래처코드 + 원본 YYYYMMDD
            val existing = existingByKey[key]
            val wasNew = existing == null

            // salesDate 는 immutable(val). 동일 external key(거래처+원본일자) 재수신 시 대상월도 같아
            // 보정 SalesDate 값이 불변이므로, 기존 row 는 salesDate 재설정 없이 금액만 갱신한다.
            val entity = existing ?: DailySalesHistory(
                sapAccountCode = sapCode,
                salesDate = salesDateColumn,        // 보정된 날짜 (today/말일)
                externalKey = key,
            ).also { existingByKey[key] = it }

            entity.erpSalesAmount = orora.erpSalesAmount?.toDouble()
            entity.erpDistributionAmount = orora.erpDistributionAmount?.toDouble()
            // 레거시 LedgerAmount 미매핑 동등: 일별 ledger 는 채우지 않는다 (월별 TotalLedger=Σnull=0).
            entity.account = account
            entity.accountSfid = account.sfid

            toSave += entity
            outcomes += DailyUpsertOutcome(entity, sapCode, wasNew)
        }
        dailySalesHistoryRepository.saveAll(toSave)
        return outcomes
    }

    /**
     * 일별 적재 직후 월별 합계를 갱신 — 레거시 DailyErpSalesInfoTriggerHandler 부수효과 동등.
     *
     * 거래처+월 단위로 묶어, 레거시의 insert 트리거(합산) → update 트리거(마지막값) 순차 발동을 재현한다:
     * - 신규(insert) 일별 row 묶음 → `abcClosingSum/shipClosingSum/totalLedger` 를 **합산**으로 set.
     * - 기존(update) 일별 row 묶음 → 같은 컬럼을 **마지막 row 1건 값**으로 set (레거시 :257-259 `=`).
     * 두 묶음이 같은 거래처+월에 공존하면 insert(합산) 적용 후 update(마지막값)로 덮어쓴다 (트리거 순서 동등).
     *
     * 온도대별 마감 컬럼 / 목표(thisMonthTarget 등) / 비고(remark) / 마감(isConfirmed) 은 건드리지 않아 보존.
     */
    private fun updateMonthlyAggregate(
        salesYear: SalesYear,
        salesMonthEnum: SalesMonth,
        outcomes: List<DailyUpsertOutcome>,
        accountByCode: Map<String?, Account>,
    ): Int {
        if (outcomes.isEmpty()) return 0

        val sapCodes = outcomes.map { it.sapCode }.distinct()
        val existing = monthlySalesHistoryRepository
            .findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(listOf(salesYear), listOf(salesMonthEnum), sapCodes)
            .filter { it.externalkeyC != null }
            .associateBy { it.sapAccountCode }
            .toMutableMap()

        val toSave = LinkedHashMap<String, MonthlySalesHistory>()
        sapCodes.forEach { sapCode ->
            val key = sapCode + salesYear.value + salesMonthEnum.value
            val entity = existing[sapCode] ?: MonthlySalesHistory(
                sapAccountCode = sapCode,
                salesYear = salesYear,
                salesMonth = salesMonthEnum,
                externalkeyC = key,
            ).also {
                accountByCode[sapCode]?.let { acc -> it.account = acc; it.accountSfid = acc.sfid }
            }

            // 레거시 null→0 초기화 (TriggerHandler:90-97, 236-238).
            if (entity.abcClosingSumAmount == null) entity.abcClosingSumAmount = 0.0
            if (entity.shipClosingSumAmount == null) entity.shipClosingSumAmount = 0.0
            if (entity.totalLedgerAmount == null) entity.totalLedgerAmount = java.math.BigDecimal.ZERO

            val rows = outcomes.filter { it.sapCode == sapCode }

            // 1) insert 경로: 신규 row 묶음을 합산 (레거시 beforeInsert :99-108).
            val inserted = rows.filter { it.wasNew }
            if (inserted.isNotEmpty()) {
                entity.abcClosingSumAmount = inserted.sumOf { it.entity.erpSalesAmount ?: 0.0 }
                entity.shipClosingSumAmount = inserted.sumOf { it.entity.erpDistributionAmount ?: 0.0 }
                entity.totalLedgerAmount = java.math.BigDecimal.valueOf(
                    inserted.sumOf { it.entity.ledgerAmount ?: 0.0 }
                )
            }

            // 2) update 경로: 기존 row 묶음의 마지막 1건 값으로 덮어쓰기 (레거시 beforeUpdate :257-259 `=`).
            val updated = rows.filter { !it.wasNew }
            if (updated.isNotEmpty()) {
                val last = updated.last().entity
                entity.abcClosingSumAmount = last.erpSalesAmount ?: 0.0
                entity.shipClosingSumAmount = last.erpDistributionAmount ?: 0.0
                entity.totalLedgerAmount = java.math.BigDecimal.valueOf(last.ledgerAmount ?: 0.0)
            }

            toSave[sapCode] = entity
        }
        monthlySalesHistoryRepository.saveAll(toSave.values.toList())
        return toSave.size
    }

    /**
     * 레거시 SalesDate 보정 (Queueable:108-119): 대상 매출월(YYYYMM)이 today 의 연월과 같으면 today,
     * 다르면 그 달 말일. external key 는 레거시와 동일하게 **원본 YYYYMMDD(=보정 날짜의 token)** 를 사용한다.
     */
    private fun resolveSalesDate(salesMonth: String, today: LocalDate = LocalDate.now()): ResolvedSalesDate {
        val targetYear = salesMonth.substring(0, 4).toInt()
        val targetMonth = salesMonth.substring(4, 6).toInt()
        val date = if (targetYear == today.year && targetMonth == today.monthValue) {
            today
        } else {
            LocalDate.of(targetYear, targetMonth, 1).plusMonths(1).minusDays(1)
        }
        return ResolvedSalesDate(date)
    }

    private data class ResolvedSalesDate(val date: LocalDate) {
        /** daily_sales_history.sales_date 저장값 (YYYYMMDD 문자열). */
        val token: String = "%04d%02d%02d".format(date.year, date.monthValue, date.dayOfMonth)
    }

    private fun externalKey(orora: OroraDailySalesHistory): String =
        orora.sapAccountCode.removePrefix(OroraAccountRange.ACCOUNT_CODE_PREFIX) + orora.salesDate
}
