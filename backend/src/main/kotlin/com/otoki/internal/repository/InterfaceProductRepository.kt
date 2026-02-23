package com.otoki.internal.repository

import com.otoki.internal.entity.InterfaceProduct
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 인터페이스 제품 Repository
 */
@Repository
interface InterfaceProductRepository : JpaRepository<InterfaceProduct, Int> {

    fun findByProductCode(productCode: String): InterfaceProduct?
}
