package com.otoki.powersales.domain.foundation.product.service

import com.otoki.powersales.domain.foundation.product.dto.response.Category2Node
import com.otoki.powersales.domain.foundation.product.dto.response.CategoryTree
import com.otoki.powersales.domain.foundation.product.dto.response.ProductDetail
import com.otoki.powersales.domain.foundation.product.dto.response.ProductListItem
import com.otoki.powersales.domain.foundation.product.dto.response.ProductListResponse
import com.otoki.powersales.domain.foundation.product.entity.ProductBarcode
import com.otoki.powersales.domain.foundation.product.exception.ProductNotFoundException
import com.otoki.powersales.domain.foundation.product.repository.ProductBarcodeRepository
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminProductService(
    private val productRepository: ProductRepository,
    private val productBarcodeRepository: ProductBarcodeRepository
) {

    /**
     * 관리자 제품 목록 조회.
     *
     * 검색 조건으로 페이지를 뽑은 뒤, 해당 페이지 제품들의 바코드를 1회 일괄 조회해
     * 행마다 앱 노출/주문 진단([ProductAppVisibilityEvaluator])을 채운다 (N+1 회피).
     * [appSearchable] 은 목록 자체를 앱 노출 여부로 좁히는 필터로, 페이지 총건수까지 맞아야 하므로
     * 메모리가 아니라 검색 쿼리 술어로 처리된다.
     */
    fun getProducts(
        keyword: String?,
        category1: String?,
        category2: String?,
        category3: String?,
        productStatus: String?,
        appSearchable: Boolean? = null,
        page: Int,
        size: Int
    ): ProductListResponse {
        val pageable = PageRequest.of(page, size, Sort.by("name").ascending())
        val productPage = productRepository.searchForAdmin(
            keyword = keyword,
            category1 = category1,
            category2 = category2,
            category3 = category3,
            productStatus = productStatus,
            appSearchable = appSearchable,
            pageable = pageable
        )

        val barcodesByProductId = loadBarcodesByProductId(productPage.content.map { it.id })

        return ProductListResponse(
            content = productPage.content.map { product ->
                ProductListItem.Companion.from(
                    product = product,
                    appVisibility = ProductAppVisibilityEvaluator.evaluate(
                        product,
                        barcodesByProductId[product.id].orEmpty()
                    )
                )
            },
            page = page,
            size = size,
            totalElements = productPage.totalElements,
            totalPages = productPage.totalPages
        )
    }

    /**
     * 관리자 제품 상세 조회.
     *
     * 바코드 목록을 함께 반환하며, 같은 바코드로 앱 노출/주문 진단을 산출한다.
     * 진단은 소프트 삭제 여부를 거르지 않은 **원본 바코드 전량** 기준이다 — 앱 검색 쿼리가
     * `is_deleted` 를 보지 않으므로, 화면 표시용으로 걸러낸 목록으로 판정하면 실제와 어긋난다.
     */
    fun getProductDetail(productCode: String): ProductDetail {
        val product = productRepository.findByProductCode(productCode)
            ?: throw ProductNotFoundException(productCode)
        val barcodes = productBarcodeRepository.findByProductId(product.id)
        return ProductDetail.Companion.from(product, barcodes)
    }

    private fun loadBarcodesByProductId(productIds: List<Long>): Map<Long, List<ProductBarcode>> {
        if (productIds.isEmpty()) return emptyMap()
        return productBarcodeRepository.findByProductIdIn(productIds)
            .groupBy { it.productId ?: 0L }
    }

    fun getCategories(): List<CategoryTree> {
        val rows = productRepository.findDistinctCategories()

        return rows
            .groupBy { it.category1 }
            .map { (cat1, cat1Rows) ->
                CategoryTree(
                    category1 = cat1,
                    children = cat1Rows
                        .groupBy { it.category2 }
                        .map { (cat2, cat2Rows) ->
                            Category2Node(
                                category2 = cat2,
                                children = cat2Rows.map { it.category3 }.sorted()
                            )
                        }
                        .sortedBy { it.category2 }
                )
            }
            .sortedBy { it.category1 }
    }
}
