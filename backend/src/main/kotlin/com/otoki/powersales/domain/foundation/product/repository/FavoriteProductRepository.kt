package com.otoki.powersales.domain.foundation.product.repository

import com.otoki.powersales.domain.foundation.product.entity.FavoriteProduct
import com.otoki.powersales.domain.foundation.product.entity.ProductFavoriteId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 즐겨찾기 제품 Repository
 *
 * V1 스키마(`product_favorites`, 복합키 `(employeecode, productcode)`) 기준.
 * 레거시 `productFavoriteMapper.xml` 정합 — 사번(empcode__c) + 제품코드 단위로 조회/존재확인/삭제한다.
 */
@Repository
interface FavoriteProductRepository : JpaRepository<FavoriteProduct, ProductFavoriteId> {

    /** 사번 기준 즐겨찾기 목록 — 최근 추가순(레거시 목록 정렬 정합). */
    fun findByEmployeeCodeOrderByCreatedAtDesc(employeeCode: String): List<FavoriteProduct>

    fun existsByEmployeeCodeAndProductCode(employeeCode: String, productCode: String): Boolean

    fun findByEmployeeCodeAndProductCode(employeeCode: String, productCode: String): FavoriteProduct?
}
