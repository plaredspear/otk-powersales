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
import java.math.BigDecimal
import java.time.LocalDate

/**
 * ORORA 일별 매출 적재 + 월별 합계 갱신의 단일 chunk 처리 (Spec #855).
 *
 * [OroraDailySalesMaterializeService] 의 루프에서 chunk 단위로 호출되며, 별도 빈으로 분리하여
 * `REQUIRES_NEW` 트랜잭션 프록시가 정상 적용되도록 한다. 한 chunk 의 일별 upsert + 월별 합계 갱신을
 * 하나의 트랜잭션으로 묶어 원자성을 보장한다 (레거시 SObject upsert + trigger 부수효과 동등, §2.3).
 *
 * ## 레거시 동작 정합 (Queueable_OroraDailySalesHistory_M1 + DailyErpSalesInfoTriggerHandler)
 * - 일별 SalesDate 는 대상월==today연월이면 `today`, 아니면 그 달 말일로 보정 (Queueable:113-119).
 * - 거래처(Account) 미매칭 row 는 일별 적재 자체를 차단 (TriggerHandler:79-80 `addError` 동등).
 * - 월별 갱신 컬럼은 `abcClosingSumAmount`(ΣERPSales) + `shipClosingSumAmount`(ΣERPDist) + `totalLedgerAmount`(ΣLedger).
 *   온도대별 마감 컬럼 / 목표 / 비고 / 마감 boolean 은 보존.
 * - 일별 LedgerAmount 는 ORORA view 미제공이라 항상 null → ORORA 적재분의 TotalLedger 기여는 0.
 *
 * ## 레거시 deviation — 월별 합계는 재합산 (사용자 결정 2026-07-09)
 * 레거시 트리거는 "이번 처리분 기준 대입" (insert 경로=신규 묶음 합산 / update 경로=마지막 1건 값 덮어쓰기,
 * TriggerHandler beforeUpdate :257-259 buggy `=` 대입) 이라 재실행 시 값이 달라져 멱등이 아니었다.
 * 신규는 적재 후 해당 거래처+월의 `daily_sales_history` 전체를 재조회하여 합계를 대입 — 재실행/부분 재적재에
 * 대해 완전 멱등. (초기 구현은 레거시 raw 재현이었으나 멱등성 확보 결정으로 교체.)
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

        val saved = upsertDaily(salesMonth, ororaRows, accountByCode)
        val monthlyUpdated = updateMonthlyAggregate(
            salesMonth, salesYear, salesMonthEnum,
            saved.map { it.sapAccountCode }.distinct(), accountByCode,
        )
        return ChunkResult(saved.size, monthlyUpdated)
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
    ): List<DailySalesHistory> {
        // 레거시 SalesDate__c 저장값은 보정(today/말일), external key 는 원본 YYYYMMDD (Queueable:115-145).
        // 둘을 분리한다: salesDate 컬럼=보정값, external_key=거래처코드+원본일자.
        val salesDateColumn = resolveSalesDate(salesMonth).token

        val externalKeys = ororaRows.map { externalKey(it) }
        val existingByKey = dailySalesHistoryRepository.findByExternalKeyIn(externalKeys.distinct())
            .associateBy { it.externalKey }
            .toMutableMap()

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

            // salesDate 는 immutable(val). 동일 external key(거래처+원본일자) 재수신 시 대상월도 같아
            // 보정 SalesDate 값이 불변이므로, 기존 row 는 salesDate 재설정 없이 금액만 갱신한다.
            val entity = existingByKey[key] ?: DailySalesHistory(
                sapAccountCode = sapCode,
                salesDate = salesDateColumn,        // 보정된 날짜 (today/말일)
                externalKey = key,
            ).also { existingByKey[key] = it }

            entity.erpSalesAmount = orora.erpSalesAmount?.toDouble()
            entity.erpDistributionAmount = orora.erpDistributionAmount?.toDouble()
            // 레거시 LedgerAmount 미매핑 동등: 일별 ledger 는 채우지 않는다.
            entity.account = account
            entity.accountSfid = account.sfid

            toSave += entity
        }
        dailySalesHistoryRepository.saveAll(toSave)
        return toSave
    }

    /**
     * 일별 적재 직후 월별 합계를 재합산 — 레거시 DailyErpSalesInfoTriggerHandler 부수효과의 멱등 대체.
     *
     * 이번 실행이 적재한 거래처들의 해당 월 `daily_sales_history` **전체**를 재조회하여
     * `abcClosingSum(ΣERPSales)/shipClosingSum(ΣERPDist)/totalLedger(ΣLedger)` 를 대입한다.
     * 방금 saveAll 한 적재분은 JPQL 실행 전 auto-flush 로 조회에 반영된다.
     *
     * 레거시 deviation (사용자 결정 2026-07-09): 레거시의 "처리분 기준 대입" (insert=합산/update=마지막 1건 값)
     * 은 재실행 시 값이 달라지는 비멱등 동작이라 재합산으로 교체. 재실행/부분 재적재 몇 번을 반복해도
     * 항상 같은 월 합계로 수렴한다.
     *
     * 온도대별 마감 컬럼 / 목표(thisMonthTarget 등) / 비고(remark) / 마감(isConfirmed) 은 건드리지 않아 보존.
     */
    private fun updateMonthlyAggregate(
        salesMonth: String,
        salesYear: SalesYear,
        salesMonthEnum: SalesMonth,
        sapCodes: List<String>,
        accountByCode: Map<String?, Account>,
    ): Int {
        if (sapCodes.isEmpty()) return 0

        val existing = monthlySalesHistoryRepository
            .findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(listOf(salesYear), listOf(salesMonthEnum), sapCodes)
            .filter { it.externalkeyC != null }
            .associateBy { it.sapAccountCode }

        // 이번 chunk 처리분 외의 기존 적재분(다른 날짜 row 등)까지 포함한 해당 월 전량 재조회.
        val monthRowsByCode = dailySalesHistoryRepository
            .findBySapAccountCodeInAndSalesDateStartingWith(sapCodes, salesMonth)
            .groupBy { it.sapAccountCode }

        val toSave = mutableListOf<MonthlySalesHistory>()
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

            val rows = monthRowsByCode[sapCode].orEmpty()
            entity.abcClosingSumAmount = rows.sumOf { it.erpSalesAmount ?: 0.0 }
            entity.shipClosingSumAmount = rows.sumOf { it.erpDistributionAmount ?: 0.0 }
            entity.totalLedgerAmount = BigDecimal.valueOf(rows.sumOf { it.ledgerAmount ?: 0.0 })

            toSave += entity
        }
        monthlySalesHistoryRepository.saveAll(toSave)
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
