package com.otoki.internal.order.repository

import com.otoki.internal.order.entity.OrderItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 주문 제품 항목 Repository
 */
@Repository
interface OrderItemRepository : JpaRepository<OrderItem, Long> {

    fun findByOrderId(orderId: Long): List<OrderItem>

    // TODO: Spec #XXX에서 활성화 — findOrderHistoryProducts (Product 엔티티 필드 매핑 확인 후)
    // TODO: Spec #XXX에서 활성화 — countOrderHistoryProducts
}
