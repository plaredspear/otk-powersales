package com.otoki.internal.repository

import com.otoki.internal.entity.ProductSyncBuffer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 제품 동기화 버퍼 Repository
 */
@Repository
interface ProductSyncBufferRepository : JpaRepository<ProductSyncBuffer, Int> {

    fun findByProductCode(productCode: String): ProductSyncBuffer?
}
