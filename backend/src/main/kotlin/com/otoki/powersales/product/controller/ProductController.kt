package com.otoki.powersales.product.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.product.dto.response.ProductCategoryGroup
import com.otoki.powersales.product.dto.response.ProductDetail
import com.otoki.powersales.product.dto.response.ProductDto
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.product.service.ProductService
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 제품 검색 API Controller
 */
@RestController
@RequestMapping("/api/v1/mobile/products")
class ProductController(
    private val productService: ProductService
) {

    /**
     * 제품 검색
     * GET /api/v1/mobile/products/search
     *
     * @param query 검색어 (제품명/제품코드/바코드)
     * @param type 검색 유형 (text 또는 barcode, 기본: text)
     * @param page 페이지 번호 (기본: 0)
     * @param size 페이지 크기 (기본: 20)
     */
    @GetMapping("/search")
    fun searchProducts(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam query: String,
        @RequestParam(defaultValue = "text") type: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<Page<ProductDto>>> {
        val result = productService.searchProducts(query, type, page, size)
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }

    /**
     * 제품 필터 검색 (레거시 제품추가 팝업 정합)
     * GET /api/v1/mobile/products/search/filter
     *
     * 제품명/바코드/중분류/소분류 조합 검색. 모든 조건 선택적이며 비어 있으면 전체 orderable 제품 반환.
     *
     * @param productName 제품명 (부분일치)
     * @param barcode 제품바코드 (부분일치)
     * @param category2 중분류
     * @param category3 소분류
     * @param page 페이지 번호 (기본: 0)
     * @param size 페이지 크기 (기본: 20)
     */
    @GetMapping("/search/filter")
    fun searchProductsByFilter(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) productName: String?,
        @RequestParam(required = false) barcode: String?,
        @RequestParam(required = false) category2: String?,
        @RequestParam(required = false) category3: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<Page<ProductDto>>> {
        val result = productService.searchProductsByFilter(productName, barcode, category2, category3, page, size)
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }

    /**
     * 제품 카테고리(중분류→소분류) 목록 조회
     * GET /api/v1/mobile/products/categories
     *
     * 제품추가 팝업의 중분류/소분류 드롭다운 소스.
     */
    @GetMapping("/categories")
    fun getProductCategories(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<List<ProductCategoryGroup>>> {
        val result = productService.getOrderableCategories()
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }

    /**
     * 제품 상세 조회
     * GET /api/v1/mobile/products/{productCode}
     *
     * @param productCode 제품코드
     */
    @GetMapping("/{productCode}")
    fun getProductDetail(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable productCode: String
    ): ResponseEntity<ApiResponse<ProductDetail>> {
        val result = productService.getProductDetail(productCode)
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }
}
