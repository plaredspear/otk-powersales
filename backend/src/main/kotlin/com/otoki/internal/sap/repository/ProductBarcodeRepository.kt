package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.ProductBarcode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 제품 바코드 Repository
 */
@Repository
interface ProductBarcodeRepository : JpaRepository<ProductBarcode, Int> {

    fun findByProductSfid(productSfid: String): List<ProductBarcode>

    fun findByBarcode(barcode: String): List<ProductBarcode>

    fun findByCustomKey(customKey: String): ProductBarcode?
}
