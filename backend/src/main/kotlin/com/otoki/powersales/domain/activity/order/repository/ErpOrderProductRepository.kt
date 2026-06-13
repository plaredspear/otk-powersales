package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.domain.activity.order.entity.ErpOrderProduct
import org.springframework.data.jpa.repository.JpaRepository

interface ErpOrderProductRepository : JpaRepository<ErpOrderProduct, Long> {

    fun findByExternalKey(externalKey: String): ErpOrderProduct?

    fun findBySapOrderNumberOrderByLineNumberAsc(sapOrderNumber: String): List<ErpOrderProduct>
}
