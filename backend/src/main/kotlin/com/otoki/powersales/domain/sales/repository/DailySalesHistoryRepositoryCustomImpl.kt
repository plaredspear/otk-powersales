package com.otoki.powersales.domain.sales.repository

import com.otoki.powersales.domain.sales.entity.QDailySalesHistory.Companion.dailySalesHistory
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory

class DailySalesHistoryRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : DailySalesHistoryRepositoryCustom {

    override fun sumMonthlyBySapAccountCodeIn(
        sapAccountCodes: Collection<String>,
        salesMonth: String,
    ): List<DailySalesMonthlySum> =
        sumMonthlyWhere(
            dailySalesHistory.sapAccountCode.`in`(sapAccountCodes),
            dailySalesHistory.salesDate.startsWith(salesMonth),
        )

    override fun sumMonthlyBySapAccountCodeBetween(
        fromCode: String,
        toCode: String,
        salesMonth: String,
    ): List<DailySalesMonthlySum> =
        sumMonthlyWhere(
            dailySalesHistory.sapAccountCode.between(fromCode, toCode),
            dailySalesHistory.salesDate.startsWith(salesMonth),
        )

    /** 거래처별 월 금액 합계 집계 공통 쿼리 — where 조건만 호출측이 결정한다. */
    private fun sumMonthlyWhere(vararg predicates: Predicate): List<DailySalesMonthlySum> =
        queryFactory
            .select(
                Projections.constructor(
                    DailySalesMonthlySum::class.java,
                    dailySalesHistory.sapAccountCode,
                    dailySalesHistory.erpSalesAmount.sumAggregate(),
                    dailySalesHistory.erpDistributionAmount.sumAggregate(),
                    dailySalesHistory.ledgerAmount.sumAggregate(),
                ),
            )
            .from(dailySalesHistory)
            .where(*predicates)
            .groupBy(dailySalesHistory.sapAccountCode)
            .fetch()
}
