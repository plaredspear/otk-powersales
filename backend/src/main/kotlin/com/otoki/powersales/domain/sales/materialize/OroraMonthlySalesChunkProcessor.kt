package com.otoki.powersales.domain.sales.materialize

import com.otoki.orora.entity.OroraMonthlySalesHistory
import com.otoki.orora.repository.OroraMonthlySalesHistoryRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.sales.entity.MonthlySalesHistory
import com.otoki.powersales.domain.sales.enums.SalesMonth
import com.otoki.powersales.domain.sales.enums.SalesYear
import com.otoki.powersales.domain.sales.repository.MonthlySalesHistoryRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * ORORA 월별 매출 적재의 단일 chunk 처리 (Spec #855).
 *
 * [OroraMonthlySalesMaterializeService] 의 루프에서 chunk 단위로 호출되며, 별도 빈으로 분리하여
 * `REQUIRES_NEW` 트랜잭션 프록시가 정상 적용되도록 한다 (self-invocation 시 트랜잭션 미적용 회피).
 * 한 chunk 의 ORORA 조회 → RDS upsert 를 독립 트랜잭션으로 격리한다.
 */
@Component
class OroraMonthlySalesChunkProcessor(
    private val ororaMonthlyRepository: OroraMonthlySalesHistoryRepository,
    private val monthlySalesHistoryRepository: MonthlySalesHistoryRepository,
    private val accountRepository: AccountRepository,
) {

    data class ChunkResult(val fetched: Int, val upserted: Int, val unmatched: Int)

    /**
     * 단일 chunk 의 ORORA 월별 조회 + `monthly_sales_history` upsert (마감 금액 컬럼만).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun process(
        salesMonth: String,
        salesYear: SalesYear,
        salesMonthEnum: SalesMonth,
        fromCode: String,
        toCode: String,
    ): ChunkResult {
        val ororaRows = ororaMonthlyRepository
            .findBySalesDateAndSapAccountCodeBetween(salesMonth, fromCode, toCode)
        if (ororaRows.isEmpty()) return ChunkResult(0, 0, 0)

        val sapCodes = ororaRows.map { it.sapAccountCode.removePrefix(OroraAccountRange.ACCOUNT_CODE_PREFIX) }.distinct()
        val accountByCode = accountRepository.findByExternalKeyIn(sapCodes).associateBy { it.externalKey }
        val existingByKey = monthlySalesHistoryRepository
            .findBySalesYearInAndSalesMonthInAndSapAccountCodeIn(listOf(salesYear), listOf(salesMonthEnum), sapCodes)
            .filter { it.externalkeyC != null }
            .associateBy { it.externalkeyC!! }
            .toMutableMap()

        var unmatched = 0
        val toSave = mutableListOf<MonthlySalesHistory>()

        ororaRows.forEach { orora ->
            val sapCode = orora.sapAccountCode.removePrefix(OroraAccountRange.ACCOUNT_CODE_PREFIX)
            val key = sapCode + salesYear.value + salesMonthEnum.value
            val account = accountByCode[sapCode]
            if (account == null) unmatched++

            val entity = existingByKey[key] ?: MonthlySalesHistory(
                sapAccountCode = sapCode,
                salesYear = salesYear,
                salesMonth = salesMonthEnum,
                externalkeyC = key,
            ).also { existingByKey[key] = it }

            applyMonthly(entity, orora, sapCode, salesYear, salesMonthEnum, key, account)
            toSave += entity
        }

        monthlySalesHistoryRepository.saveAll(toSave)
        return ChunkResult(ororaRows.size, toSave.size, unmatched)
    }

    /**
     * ORORA 월별 row → `monthly_sales_history` 마감 금액 컬럼 적용. 운영 입력 컬럼(target/remark/confirm)은 미변경.
     */
    private fun applyMonthly(
        entity: MonthlySalesHistory,
        orora: OroraMonthlySalesHistory,
        sapCode: String,
        salesYear: SalesYear,
        salesMonthEnum: SalesMonth,
        key: String,
        account: Account?,
    ) {
        entity.sapAccountCode = sapCode
        entity.salesYear = salesYear
        entity.salesMonth = salesMonthEnum
        entity.externalkeyC = key
        // 레거시 doPost 동등: 개별 마감 1~4 컬럼은 null → 0 으로 적재 ((objMap.get(..) == null) ? 0 : ..).
        entity.abcClosingAmount1 = orora.abcClosingAmount1?.toDouble() ?: 0.0
        entity.abcClosingAmount2 = orora.abcClosingAmount2?.toDouble() ?: 0.0
        entity.abcClosingAmount3 = orora.abcClosingAmount3?.toDouble() ?: 0.0
        entity.abcClosingAmount4 = orora.abcClosingAmount4?.toDouble() ?: 0.0
        entity.shipClosingAmount1 = orora.shipClosingAmount1?.toDouble() ?: 0.0
        entity.shipClosingAmount2 = orora.shipClosingAmount2?.toDouble() ?: 0.0
        entity.shipClosingAmount3 = orora.shipClosingAmount3?.toDouble() ?: 0.0
        entity.shipClosingAmount4 = orora.shipClosingAmount4?.toDouble() ?: 0.0
        entity.abcClosingSumAmount = orora.abcClosingSumAmount.toDouble()
        entity.shipClosingSumAmount = orora.shipClosingSumAmount.toDouble()
        if (account != null) {
            entity.account = account
            entity.accountSfid = account.sfid
        }
    }
}
