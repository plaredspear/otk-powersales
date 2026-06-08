package com.otoki.powersales.product.service

import com.otoki.powersales.product.dto.response.ProductCategoryGroup
import com.otoki.powersales.product.dto.response.ProductDetail
import com.otoki.powersales.product.dto.response.ProductDto
import com.otoki.powersales.product.exception.InvalidSearchParameterException
import com.otoki.powersales.product.exception.InvalidSearchTypeException
import com.otoki.powersales.product.exception.ProductNotFoundException
import com.otoki.powersales.product.repository.ProductRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 제품 검색 Service
 */
@Service
@Transactional(readOnly = true)
class ProductService(
    private val productRepository: ProductRepository
) {

    companion object {
        private const val MIN_TEXT_QUERY_LENGTH = 2
        private const val MIN_BARCODE_LENGTH = 8
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 100

        private val NUMERIC_PATTERN = Regex("^\\d+$")
        private val VALID_SEARCH_TYPES = setOf("text", "barcode")
    }

    /**
     * 제품 검색
     *
     * @param query 검색어
     * @param type 검색 유형 (text 또는 barcode)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 검색 결과 Page
     */
    fun searchProducts(
        query: String,
        type: String,
        page: Int,
        size: Int
    ): Page<ProductDto> {
        // 검색 유형 검증
        validateSearchType(type)

        // 페이지네이션 파라미터 검증
        validatePagination(page, size)

        // 검색어 검증 및 실행
        val pageable = PageRequest.of(page, size)

        val rowPage = when (type) {
            "barcode" -> {
                validateBarcodeQuery(query)
                productRepository.findByLogisticsBarcode(query.trim(), pageable)
            }
            else -> {
                validateTextQuery(query)
                val trimmedQuery = query.trim()
                if (NUMERIC_PATTERN.matches(trimmedQuery)) {
                    productRepository.searchByTextIncludingBarcode(trimmedQuery, pageable)
                } else {
                    productRepository.searchByText(trimmedQuery, pageable)
                }
            }
        }

        return rowPage.map { ProductDto.from(it.product, it.barcode) }
    }

    /**
     * 제품 필터 검색 — 레거시 제품추가 팝업(`selectProduct`) 정합.
     *
     * 제품명/바코드/중분류/소분류 조합으로 검색하며 모든 조건은 선택적이다.
     * 모든 조건이 비어 있으면 orderable 제품 전체를 페이지로 반환한다(레거시 빈 검색 동작).
     *
     * @param productName 제품명(부분일치, 제품코드 포함)
     * @param barcode 제품바코드(부분일치)
     * @param category2 중분류
     * @param category3 소분류
     */
    fun searchProductsByFilter(
        productName: String?,
        barcode: String?,
        category2: String?,
        category3: String?,
        page: Int,
        size: Int
    ): Page<ProductDto> {
        validatePagination(page, size)

        val pageable = PageRequest.of(page, size)
        val rowPage = productRepository.searchByFilter(
            productName = productName?.trim()?.ifBlank { null },
            barcode = barcode?.trim()?.ifBlank { null },
            category2 = category2?.trim()?.ifBlank { null },
            category3 = category3?.trim()?.ifBlank { null },
            pageable = pageable
        )

        return rowPage.map { ProductDto.from(it.product, it.barcode) }
    }

    /**
     * 모바일 제품추가 팝업 중분류→소분류 드롭다운 소스 조회.
     */
    fun getOrderableCategories(): List<ProductCategoryGroup> {
        return productRepository.findOrderableCategories()
            .groupBy { it.category2 }
            .map { (middle, rows) ->
                ProductCategoryGroup(
                    middle = middle,
                    subs = rows.map { it.category3 }.distinct().sorted()
                )
            }
            .sortedBy { it.middle }
    }

    /**
     * 제품 상세 조회 (제품코드 단건).
     *
     * 레거시 `product/search/detail.jsp` 대응. 검색 결과보다 많은 필드를 반환한다.
     *
     * @param productCode 제품코드
     * @return 제품 상세 (검증된 이미지 URL 결합 로직을 갖춘 공용 ProductDetail DTO 재사용)
     * @throws ProductNotFoundException 제품코드에 해당하는 제품이 없을 때
     */
    fun getProductDetail(productCode: String): ProductDetail {
        val product = productRepository.findByProductCode(productCode)
            ?: throw ProductNotFoundException(productCode)
        return ProductDetail.from(product)
    }

    private fun validateSearchType(type: String) {
        if (type !in VALID_SEARCH_TYPES) {
            throw InvalidSearchTypeException()
        }
    }

    private fun validateTextQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            throw InvalidSearchParameterException("검색어를 입력해주세요")
        }
        if (trimmed.length < MIN_TEXT_QUERY_LENGTH) {
            throw InvalidSearchParameterException("검색어는 ${MIN_TEXT_QUERY_LENGTH}자 이상이어야 합니다")
        }
    }

    private fun validateBarcodeQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            throw InvalidSearchParameterException("바코드를 입력해주세요")
        }
        if (!NUMERIC_PATTERN.matches(trimmed) || trimmed.length < MIN_BARCODE_LENGTH) {
            throw InvalidSearchParameterException("유효하지 않은 바코드 형식입니다")
        }
    }

    private fun validatePagination(page: Int, size: Int) {
        if (page < 0) {
            throw InvalidSearchParameterException("페이지 번호는 0 이상이어야 합니다")
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw InvalidSearchParameterException("페이지 크기는 1~${MAX_PAGE_SIZE} 범위여야 합니다")
        }
    }
}
