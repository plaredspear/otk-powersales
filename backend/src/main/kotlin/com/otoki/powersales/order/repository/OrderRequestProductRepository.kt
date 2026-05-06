package com.otoki.powersales.order.repository

import com.otoki.powersales.order.entity.OrderRequestProduct
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRequestProductRepository : JpaRepository<OrderRequestProduct, Long> {

    fun findByOrderRequest_IdOrderByLineNumberAsc(orderRequestId: Long): List<OrderRequestProduct>
}
