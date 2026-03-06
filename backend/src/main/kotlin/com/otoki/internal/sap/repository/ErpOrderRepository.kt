package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.ErpOrder
import org.springframework.data.jpa.repository.JpaRepository

interface ErpOrderRepository : JpaRepository<ErpOrder, Long> {

    fun findBySapOrderNumber(sapOrderNumber: String): ErpOrder?
}
