package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRequestRepository : JpaRepository<OrderRequest, Long>, OrderRequestRepositoryCustom {

    fun findByClientRequestId(clientRequestId: String): OrderRequest?

    fun existsByOrderRequestNumber(orderRequestNumber: String): Boolean
}
