package com.otoki.internal.repository

import com.otoki.internal.entity.OrderItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 주문 제품 항목 Repository
 */
@Repository
interface OrderItemRepository : JpaRepository<OrderItem, Long> {

    fun findByOrderId(orderId: Long): List<OrderItem>
}
