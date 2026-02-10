package com.otoki.internal.repository

import com.otoki.internal.entity.OrderRejection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 주문 반려 제품 Repository
 */
@Repository
interface OrderRejectionRepository : JpaRepository<OrderRejection, Long> {

    fun findByOrderId(orderId: Long): List<OrderRejection>
}
