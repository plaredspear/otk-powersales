package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OrderRequestProductRepository : JpaRepository<OrderRequestProduct, Long> {

    @Query(
        "SELECT orp FROM OrderRequestProduct orp " +
            "LEFT JOIN FETCH orp.product " +
            "WHERE orp.orderRequest.id = :orderRequestId " +
            "ORDER BY orp.lineNumber ASC"
    )
    fun findByOrderRequest_IdOrderByLineNumberAsc(@Param("orderRequestId") orderRequestId: Long): List<OrderRequestProduct>
}
