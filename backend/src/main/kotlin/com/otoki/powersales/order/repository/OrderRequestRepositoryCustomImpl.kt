package com.otoki.powersales.order.repository

import com.otoki.powersales.account.entity.QAccount.Companion.account
import com.otoki.powersales.order.entity.OrderRequest
import com.otoki.powersales.order.enums.OrderRequestStatus
import com.otoki.powersales.order.entity.QOrderRequest.Companion.orderRequest
import com.otoki.powersales.order.entity.QOrderRequestProduct.Companion.orderRequestProduct
import com.otoki.powersales.product.entity.QProduct.Companion.product
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.ComparableExpressionBase
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate
import java.time.LocalDateTime

class OrderRequestRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : OrderRequestRepositoryCustom {

    override fun findMyOrderRequests(
        employeeId: Long,
        accountId: Long?,
        status: OrderRequestStatus?,
        deliveryDateFrom: LocalDate,
        deliveryDateTo: LocalDate,
        sortBy: String,
        sortDir: String,
        limit: Int,
    ): List<OrderRequest> {
        val where = BooleanBuilder()
            .and(orderRequest.employee.id.eq(employeeId))
            .and(orderRequest.deliveryDate.goe(deliveryDateFrom))
            .and(orderRequest.deliveryDate.loe(deliveryDateTo))

        if (accountId != null) where.and(orderRequest.account.id.eq(accountId))
        if (status != null) where.and(orderRequest.orderRequestStatus.eq(status))

        return queryFactory
            .selectFrom(orderRequest)
            .leftJoin(orderRequest.account, account).fetchJoin()
            .where(where)
            .orderBy(buildOrderSpecifier(sortBy, sortDir))
            .limit(limit.toLong())
            .fetch()
    }

    override fun findOrderHistory(
        employeeId: Long,
        accountCode: String,
        orderDateFrom: LocalDateTime,
        orderDateToExclusive: LocalDateTime,
    ): List<OrderHistoryRow> {
        val where = BooleanBuilder()
            .and(orderRequest.employee.id.eq(employeeId))
            .and(orderRequest.account.externalKey.eq(accountCode))
            .and(orderRequest.orderDate.goe(orderDateFrom))
            .and(orderRequest.orderDate.lt(orderDateToExclusive))
            .and(orderRequest.isDeleted.isNull.or(orderRequest.isDeleted.eq(false)))

        return queryFactory
            .select(orderRequest.orderDate, orderRequestProduct.productCode, product.name)
            .from(orderRequestProduct)
            .join(orderRequestProduct.orderRequest, orderRequest)
            .leftJoin(orderRequestProduct.product, product)
            .where(where)
            .orderBy(orderRequest.orderDate.desc())
            .fetch()
            .map { tuple ->
                OrderHistoryRow(
                    orderDate = tuple.get(orderRequest.orderDate),
                    productCode = tuple.get(orderRequestProduct.productCode),
                    productName = tuple.get(product.name),
                )
            }
    }

    private fun buildOrderSpecifier(sortBy: String, sortDir: String): OrderSpecifier<*> {
        val direction = if (sortDir.equals("ASC", ignoreCase = true)) Order.ASC else Order.DESC
        val target: ComparableExpressionBase<*> = when (sortBy) {
            "orderDate" -> orderRequest.orderDate
            "deliveryDate" -> orderRequest.deliveryDate
            "totalAmount" -> orderRequest.totalAmount
            else -> orderRequest.orderDate
        }
        return OrderSpecifier(direction, target)
    }
}
