package com.otoki.powersales.domain.foundation.product.repository

import com.otoki.powersales.domain.foundation.product.entity.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductRepositoryCustom {

    fun searchByText(query: String, pageable: Pageable): Page<ProductSearchRow>

    fun searchByTextIncludingBarcode(query: String, pageable: Pageable): Page<ProductSearchRow>

    /**
     * 바코드 스캔 검색 — 레거시 `selectProduct` 의 바코드 매칭과 정합.
     * 소비자 바코드(ProductBarcode.barcode)에 대한 부분일치(LIKE) + orderable 필터를 적용한다.
     * (물류 바코드 `Product.logisticsBarcode` 가 아니라, 스캐너가 읽는 단위별 바코드 컬럼을 조회한다.)
     */
    fun findByBarcode(barcode: String, pageable: Pageable): Page<ProductSearchRow>

    /**
     * 주문 작성용 제품 검색 — 레거시 주문 `selectProduct`(searchWord) 정합.
     * 단일 검색어를 제품명/제품코드/소비자 바코드(ProductBarcode.barcode) OR 부분일치로 매칭하며
     * orderable 필터 + 선택적 중분류/소분류를 적용한다. 주문에 필요한 전체 필드를 함께 반환한다.
     */
    fun searchForOrder(
        query: String,
        category2: String?,
        category3: String?,
        pageable: Pageable
    ): Page<ProductSearchRow>

    /**
     * 레거시 제품추가 팝업(productMapper.xml `selectProduct`) 정합 — 제품명/바코드/중분류/소분류 조합 검색.
     * 모든 조건은 선택적이며, 모두 비어 있으면 orderable 제품 전체를 페이지로 반환한다.
     */
    fun searchByFilter(
        productName: String?,
        barcode: String?,
        category2: String?,
        category3: String?,
        pageable: Pageable
    ): Page<ProductSearchRow>

    fun searchForAdmin(
        keyword: String?,
        category1: String?,
        category2: String?,
        category3: String?,
        productStatus: String?,
        pageable: Pageable
    ): Page<Product>

    fun findDistinctCategories(): List<CategoryRow>

    /**
     * 모바일 제품추가 팝업의 중분류(category2)→소분류(category3) 드롭다운 소스.
     *
     * 레거시 `selectMiddleProduct`/`selectSmallProduct` 정합 — 발주가능(가정/업소·바코드·활성) 필터 없이
     * product 테이블의 `category2 IS NOT NULL` distinct (category2, category3) 조합을 반환한다.
     * (제품 목록 검색은 별도로 orderable 필터를 유지하므로, 드롭다운의 일부 중분류는 선택 시 0건이 될 수 있다 — 레거시 동등.)
     */
    fun findCategoryGroups(): List<CategoryGroupRow>
}

data class CategoryRow(
    val category1: String,
    val category2: String,
    val category3: String
)

/** 모바일 제품추가 드롭다운용 중분류(category2)/소분류(category3) 조합. 소분류 미지정 제품은 category3=null. */
data class CategoryGroupRow(
    val category2: String,
    val category3: String?
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
