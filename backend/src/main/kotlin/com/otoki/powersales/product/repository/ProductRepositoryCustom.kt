package com.otoki.powersales.product.repository

import com.otoki.powersales.product.entity.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductRepositoryCustom {

    fun searchByText(query: String, pageable: Pageable): Page<ProductSearchRow>

    fun searchByTextIncludingBarcode(query: String, pageable: Pageable): Page<ProductSearchRow>

    /** 바코드 정확 일치 검색 (logistics_barcode). */
    fun findByLogisticsBarcode(logisticsBarcode: String, pageable: Pageable): Page<ProductSearchRow>

    fun searchForAdmin(
        keyword: String?,
        category1: String?,
        category2: String?,
        category3: String?,
        productStatus: String?,
        pageable: Pageable
    ): Page<Product>

    fun findDistinctCategories(): List<CategoryRow>
}

data class CategoryRow(
    val category1: String,
    val category2: String,
    val category3: String
)

/**
 * 모바일 제품검색 결과 한 건 — 제품과, 발주 단위(product.unit)에 매칭되는 대표 바코드.
 *
 * 레거시 `selectProduct` 는 product 와 productbarcode 를 LEFT JOIN 해 단위 일치 바코드를
 * 한 컬럼(barCode)으로 함께 내려준다. 신규에서도 동일하게 단일 쿼리로 제품+바코드를 함께 조회한다.
 */
data class ProductSearchRow(
    val product: Product,
    val barcode: String?
)
