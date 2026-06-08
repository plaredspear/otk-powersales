package com.otoki.powersales.product.repository

import com.otoki.powersales.product.entity.Product
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 제품 Repository
 */
@Repository
interface ProductRepository : JpaRepository<Product, Long>, ProductRepositoryCustom {

    /**
     * 제품코드로 제품 조회
     */
    fun findByProductCode(productCode: String?): Product?

    /**
     * 제품코드 목록으로 일괄 조회
     */
    fun findByProductCodeIn(productCodes: List<String>): List<Product>
}
