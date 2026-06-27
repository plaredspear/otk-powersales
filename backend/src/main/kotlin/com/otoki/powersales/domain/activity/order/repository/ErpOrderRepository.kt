package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.domain.activity.order.entity.ErpOrder
import org.springframework.data.jpa.repository.JpaRepository

interface ErpOrderRepository : JpaRepository<ErpOrder, Long>, ErpOrderRepositoryCustom {

    fun findBySapOrderNumber(sapOrderNumber: String): ErpOrder?
}
