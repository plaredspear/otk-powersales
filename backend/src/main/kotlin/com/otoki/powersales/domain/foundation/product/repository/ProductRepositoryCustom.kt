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
     * 제품코드 목록으로 주문/즐겨찾기용 행을 조회한다(발주 단위 매칭 대표 바코드 포함).
     *
     * 즐겨찾기 목록이 검색 결과와 동일하게 대표 바코드(ProductBarcode.barcode)를 갖도록 하기 위한
     * 조회. orderable 필터는 적용하지 않는다(사용자가 담은 즐겨찾기는 그대로 노출).
     * 바코드가 없는 제품은 barcode = null 로 반환한다.
     */
    fun findOrderRowsByProductCodes(productCodes: Collection<String>): List<ProductSearchRow>

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

    /**
     * 전산실적(월매출) 제품/분류 필터 → 소비자 바코드(ProductBarcode.barcode) 해소.
     *
     * POS `live_tot_sales_dh` 의 `UPC_CD` 는 소비자 바코드와 매칭되므로(레거시 `selectBarcodeList`
     * → `productCd IN` 정합), 선택 제품/중분류/소분류에 해당하는 제품의 바코드 전체(단위 무관)를
     * distinct 반환한다. productIds 가 비어 있으면 분류 조건만으로, 분류가 null 이면 제품 조건만으로
     * 축소한다 (모두 지정 시 AND).
     */
    fun findBarcodesForElectronicSales(
        productIds: List<Long>,
        category2: String?,
        category3: String?,
    ): List<String>

    /**
     * 전산실적(월매출) 조회 조건의 제품 검색 — 제품명/제품코드/소비자 바코드 OR 부분일치.
     *
     * 소비자 바코드 보유 제품만 반환 (바코드가 없으면 POS `UPC_CD IN` 필터에 사용 불가).
     * 모바일 제품검색과 달리 발주가능(가정/업소·활성) 필터는 적용하지 않는다 — 매출 조회는
     * 단종/비발주 제품의 과거 실적도 대상.
     */
    fun searchForElectronicSales(keyword: String, limit: Long): List<ElectronicSalesProductLookupRow>
}

/** 전산실적 제품 검색 결과 1건 — 대표 바코드(min)와 함께 반환. */
data class ElectronicSalesProductLookupRow(
    val productId: Long,
    val name: String?,
    val productCode: String?,
    val barcode: String?,
)

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
