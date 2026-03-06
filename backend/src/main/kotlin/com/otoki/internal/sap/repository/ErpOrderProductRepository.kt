package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.ErpOrderProduct
import org.springframework.data.jpa.repository.JpaRepository

interface ErpOrderProductRepository : JpaRepository<ErpOrderProduct, Long> {

    fun findByExternalKey(externalKey: String): ErpOrderProduct?
}
