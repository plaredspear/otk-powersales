package com.otoki.powersales.order.repository

import com.otoki.powersales.order.entity.ErpOrderProduct
import org.springframework.data.jpa.repository.JpaRepository

interface ErpOrderProductRepository : JpaRepository<ErpOrderProduct, Long> {

    fun findByExternalKey(externalKey: String): ErpOrderProduct?
}
