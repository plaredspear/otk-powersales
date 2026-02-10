package com.otoki.internal.service

import com.otoki.internal.dto.response.ProductDto
import com.otoki.internal.exception.InvalidSearchParameterException
import com.otoki.internal.exception.InvalidSearchTypeException
import com.otoki.internal.repository.ProductRepository
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
                productRepository.findByBarcode(query.trim(), pageable)
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
