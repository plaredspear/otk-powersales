package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRequestProductRepository :
    JpaRepository<OrderRequestProduct, Long>,
    OrderRequestProductRepositoryCustom
