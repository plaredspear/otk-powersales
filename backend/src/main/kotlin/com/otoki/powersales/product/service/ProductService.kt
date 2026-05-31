package com.otoki.powersales.product.service

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

        val productPage = when (type) {
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

        return productPage.map { ProductDto.from(it) }
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
