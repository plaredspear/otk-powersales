package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.domain.activity.order.entity.QErpOrder.Companion.erpOrder
import com.otoki.powersales.domain.activity.order.entity.QErpOrderProduct.Companion.erpOrderProduct
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

open class ErpOrderProductRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : ErpOrderProductRepositoryCustom {

    @Transactional
    override fun deleteByErpOrderOrderDateBefore(cutoff: LocalDate): Int {
        return queryFactory
            .delete(erpOrderProduct)
            .where(
                erpOrderProduct.erpOrder.id.`in`(
                    JPAExpressions
                        .select(erpOrder.id)
                        .from(erpOrder)
                        .where(erpOrder.orderDate.lt(cutoff))
                )
            )
            .execute()
            .toInt()
    }
}
