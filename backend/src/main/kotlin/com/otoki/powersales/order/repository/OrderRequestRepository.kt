package com.otoki.powersales.order.repository

import com.otoki.powersales.order.entity.OrderRequest
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRequestRepository : JpaRepository<OrderRequest, Long>, OrderRequestRepositoryCustom {

    fun findByClientRequestId(clientRequestId: String): OrderRequest?

    fun existsByOrderRequestNumber(orderRequestNumber: String): Boolean
}
