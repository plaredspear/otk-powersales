package com.otoki.powersales.sap.repository

import com.otoki.powersales.sap.entity.ErpOrder
import org.springframework.data.jpa.repository.JpaRepository

interface ErpOrderRepository : JpaRepository<ErpOrder, Long> {

    fun findBySapOrderNumber(sapOrderNumber: String): ErpOrder?
}
