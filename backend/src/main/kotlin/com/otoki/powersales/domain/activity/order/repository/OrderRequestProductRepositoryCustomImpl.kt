package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import com.otoki.powersales.domain.activity.order.entity.QOrderRequestProduct.Companion.orderRequestProduct
import com.otoki.powersales.domain.foundation.product.entity.QProduct.Companion.product
import com.querydsl.jpa.impl.JPAQueryFactory

class OrderRequestProductRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : OrderRequestProductRepositoryCustom {

    override fun findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId: Long): List<OrderRequestProduct> {
        return queryFactory
            .selectFrom(orderRequestProduct)
            .leftJoin(orderRequestProduct.product, product).fetchJoin()
            .where(orderRequestProduct.orderRequest.id.eq(orderRequestId))
            .orderBy(orderRequestProduct.lineNumber.asc())
            .fetch()
    }
}
