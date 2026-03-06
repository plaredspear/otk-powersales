package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 제품 Repository
 */
@Repository
interface ProductRepository : JpaRepository<Product, Long>, ProductRepositoryCustom {

    /**
     * 바코드 정확 일치 검색
     */
    fun findByLogisticsBarcode(logisticsBarcode: String, pageable: Pageable): Page<Product>

    /**
     * 제품코드로 제품 조회
     */
    fun findByProductCode(productCode: String?): Product?

    /**
     * 제품코드 목록으로 일괄 조회
     */
    fun findByProductCodeIn(productCodes: List<String>): List<Product>
}
