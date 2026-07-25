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
            // 참조 주문번호가 있는 행(취소/변경 등 후속 주문)은 목록에서 제외 — 원주문 상세의 "관련 주문"으로 표시되므로
            // 목록엔 원주문(ref 없음)만 노출 (2026-07-23 사용자 결정).
            .and(erpOrder.refSapOrderNumber.isNull.or(erpOrder.refSapOrderNumber.isEmpty))

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

    override fun findAdminErpOrders(
        keyword: String?,
        deliveryDateFrom: LocalDate?,
        deliveryDateTo: LocalDate?,
        orderDateFrom: LocalDate?,
        orderDateTo: LocalDate?,
        pageable: Pageable
    ): Page<ErpOrder> {
        val where = BooleanBuilder()
            .and(erpOrder.isDeleted.isNull.or(erpOrder.isDeleted.eq(false)))

        keyword?.takeIf { it.isNotBlank() }?.trim()?.let { kw ->
            where.and(
                erpOrder.sapOrderNumber.containsIgnoreCase(kw)
                    .or(erpOrder.sapAccountCode.containsIgnoreCase(kw))
                    .or(erpOrder.sapAccountName.containsIgnoreCase(kw))
                    .or(erpOrder.employeeName.containsIgnoreCase(kw))
                    .or(erpOrder.employeeCode.containsIgnoreCase(kw))
            )
        }
        deliveryDateFrom?.let { where.and(erpOrder.deliveryRequestDate.goe(it)) }
        deliveryDateTo?.let { where.and(erpOrder.deliveryRequestDate.loe(it)) }
        orderDateFrom?.let { where.and(erpOrder.orderDate.goe(it)) }
        orderDateTo?.let { where.and(erpOrder.orderDate.loe(it)) }

        val content = queryFactory
            .selectFrom(erpOrder)
            .where(where)
            .orderBy(erpOrder.id.desc())
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
