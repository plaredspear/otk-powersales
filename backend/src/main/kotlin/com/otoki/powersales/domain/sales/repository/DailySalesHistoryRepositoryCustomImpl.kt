package com.otoki.powersales.domain.sales.repository

import com.otoki.powersales.domain.sales.entity.QDailySalesHistory.Companion.dailySalesHistory
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory

class DailySalesHistoryRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : DailySalesHistoryRepositoryCustom {

    override fun sumMonthlyBySapAccountCodeIn(
        sapAccountCodes: Collection<String>,
        salesMonth: String,
    ): List<DailySalesMonthlySum> =
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
            .where(
                dailySalesHistory.sapAccountCode.`in`(sapAccountCodes),
                dailySalesHistory.salesDate.startsWith(salesMonth),
            )
            .groupBy(dailySalesHistory.sapAccountCode)
            .fetch()
}
