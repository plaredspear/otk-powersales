package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.domain.activity.order.entity.ErpOrder
import com.otoki.powersales.domain.activity.order.entity.QErpOrder.Companion.erpOrder
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

open class ErpOrderRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : ErpOrderRepositoryCustom {

    @Transactional
    override fun deleteByOrderDateBefore(cutoff: LocalDate): Int {
        // order_date 가 NULL 인 행은 `lt` 비교가 false 라 자동 제외 (레거시 동등).
        return queryFactory
            .delete(erpOrder)
            .where(erpOrder.orderDate.lt(cutoff))
            .execute()
            .toInt()
    }

    override fun findClientOrders(
        accountId: Long,
        deliveryDate: LocalDate,
        pageable: Pageable
    ): Page<ErpOrder> {
        val where = BooleanBuilder()
            .and(erpOrder.account.id.eq(accountId))
            .and(erpOrder.deliveryRequestDate.eq(deliveryDate))
            .and(erpOrder.isDeleted.isNull.or(erpOrder.isDeleted.eq(false)))

        val content = queryFactory
            .selectFrom(erpOrder)
            .where(where)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(erpOrder.count())
            .from(erpOrder)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }
}
