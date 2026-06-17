package com.otoki.powersales.domain.foundation.product.service

import com.otoki.powersales.domain.foundation.product.dto.response.OrderProductDto
import com.otoki.powersales.domain.foundation.product.dto.response.ProductCategoryGroup
import com.otoki.powersales.domain.foundation.product.dto.response.ProductDetail
import com.otoki.powersales.domain.foundation.product.dto.response.ProductDto
import com.otoki.powersales.domain.foundation.product.exception.InvalidSearchParameterException
import com.otoki.powersales.domain.foundation.product.exception.InvalidSearchTypeException
import com.otoki.powersales.domain.foundation.product.exception.ProductNotFoundException
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
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
    private val productRepository: ProductRepository,
    private val favoriteProductService: FavoriteProductService
) {

    companion object {
        private const val MIN_TEXT_QUERY_LENGTH = 2
        private const val MIN_BARCODE_LENGTH = 8
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 100

        private val NUMERIC_PATTERN = Regex("^\\d+$")
        private val VALID_SEARCH_TYPES = setOf("text", "barcode")

        /** 소분류(category3) 드롭다운 노출 값 — 레거시 `chgSmall` 의 하드코딩 필터('가정'/'업소')와 정합. */
        private val DROPDOWN_SUB_CATEGORIES = setOf("가정", "업소")
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

        return rowPage.map { ProductDto.Companion.from(it.product, it.barcode) }
    }

    /**
     * 제품 필터 검색 — 레거시 제품추가 팝업(`selectProduct`) 정합.
     *
     * 제품명/바코드/중분류/소분류 조합으로 검색하며 모든 조건은 선택적이다.
     * 모든 조건이 비어 있으면 orderable 제품 전체를 페이지로 반환한다(레거시 빈 검색 동작).
     *
     * @param productName 제품명(부분일치) — 레거시 `selectProduct` 와 동일하게 name 컬럼만 매칭
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

        return rowPage.map { ProductDto.Companion.from(it.product, it.barcode) }
    }

    /**
     * 주문 작성용 제품 검색 — 레거시 주문 `selectProduct`(searchWord) 정합.
     *
     * 단일 검색어(제품명/제품코드/소비자 바코드 OR 부분일치)와 선택적 중분류/소분류로 검색하며,
     * 주문 라인 생성에 필요한 단가/박스입수/전용·시식 차단값을 함께 반환한다.
     *
     * @param query 검색어 (제품명/제품코드/바코드)
     * @param categoryMid 중분류(category2, 선택)
     * @param categorySub 소분류(category3, 선택)
     * @param userId 인증 사용자 — 검색 행별 즐겨찾기 여부(`isFavorite`) 표시에 사용(레거시 `product_favorites` 서브쿼리 정합)
     */
    fun searchProductsForOrder(
        query: String,
        categoryMid: String?,
        categorySub: String?,
        page: Int,
        size: Int,
        userId: Long
    ): Page<OrderProductDto> {
        validateTextQuery(query)
        validatePagination(page, size)

        val pageable = PageRequest.of(page, size)
        val rowPage = productRepository.searchForOrder(
            query = query.trim(),
            category2 = categoryMid?.trim()?.ifBlank { null },
            category3 = categorySub?.trim()?.ifBlank { null },
            pageable = pageable
        )

        val favoriteCodes = favoriteProductService.getFavoriteProductCodes(userId)
        return rowPage.map { row ->
            val dto = OrderProductDto.Companion.from(row.product, row.barcode)
            if (dto.productCode in favoriteCodes) dto.copy(isFavorite = true) else dto
        }
    }

    /**
     * 모바일 제품추가 팝업 중분류→소분류 드롭다운 소스 조회 — 레거시 `selectMiddleProduct`/`chgSmall` 정합.
     *
     * - 중분류(category2): 발주가능 필터 없이 전 제품에서 노출. 소분류가 없거나 '가정'/'업소' 가 아닌
     *   중분류도 그대로 노출한다(레거시 `selectMiddleProduct` 무필터 동등 — 선택 시 0건이 될 수 있음).
     * - 소분류(category3): 레거시 `chgSmall` 의 하드코딩 필터와 동일하게 '가정'/'업소' 만 노출한다.
     * - 중분류/소분류 모두 가나다순으로 정렬한다.
     */
    fun getProductCategories(): List<ProductCategoryGroup> {
        return productRepository.findCategoryGroups()
            .groupBy { it.category2 }
            .map { (middle, rows) ->
                ProductCategoryGroup(
                    middle = middle,
                    subs = rows.mapNotNull { it.category3 }
                        .filter { it in DROPDOWN_SUB_CATEGORIES }
                        .distinct()
                        .sorted()
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
        return ProductDetail.Companion.from(product)
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
