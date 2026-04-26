package com.otoki.powersales.sap.repository

import com.otoki.powersales.sap.entity.ErpOrderProduct
import org.springframework.data.jpa.repository.JpaRepository

interface ErpOrderProductRepository : JpaRepository<ErpOrderProduct, Long> {

    fun findByExternalKey(externalKey: String): ErpOrderProduct?
}
