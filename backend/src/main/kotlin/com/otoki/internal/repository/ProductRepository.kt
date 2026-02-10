package com.otoki.internal.repository

import com.otoki.internal.entity.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * 제품 Repository
 */
@Repository
interface ProductRepository : JpaRepository<Product, Long> {

    /**
     * 제품명 또는 제품코드로 텍스트 검색 (LIKE)
     * 검색어가 숫자가 아닌 경우 사용
     */
    @Query(
        "SELECT p FROM Product p " +
        "WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :query, '%')) " +
        "OR LOWER(p.productCode) LIKE LOWER(CONCAT('%', :query, '%')) " +
        "ORDER BY p.productName ASC"
    )
    fun searchByText(
        @Param("query") query: String,
        pageable: Pageable
    ): Page<Product>

    /**
     * 제품명, 제품코드, 바코드로 복합 텍스트 검색 (LIKE)
     * 검색어가 숫자인 경우 바코드도 함께 검색
     */
    @Query(
        "SELECT p FROM Product p " +
        "WHERE LOWER(p.productName) LIKE LOWER(CONCAT('%', :query, '%')) " +
        "OR LOWER(p.productCode) LIKE LOWER(CONCAT('%', :query, '%')) " +
        "OR p.barcode LIKE CONCAT('%', :query, '%') " +
        "ORDER BY p.productName ASC"
    )
    fun searchByTextIncludingBarcode(
        @Param("query") query: String,
        pageable: Pageable
    ): Page<Product>

    /**
     * 바코드 정확 일치 검색
     */
    fun findByBarcode(barcode: String, pageable: Pageable): Page<Product>

    /**
     * 제품코드로 제품 조회
     */
    fun findByProductCode(productCode: String): Product?

    /**
     * 제품코드 목록으로 일괄 조회
     */
    fun findByProductCodeIn(productCodes: List<String>): List<Product>
}
