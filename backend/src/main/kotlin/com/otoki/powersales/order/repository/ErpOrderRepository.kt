package com.otoki.powersales.order.repository

import com.otoki.powersales.order.entity.ErpOrder
import org.springframework.data.jpa.repository.JpaRepository

interface ErpOrderRepository : JpaRepository<ErpOrder, Long> {

    fun findBySapOrderNumber(sapOrderNumber: String): ErpOrder?
}
