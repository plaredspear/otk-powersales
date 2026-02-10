package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.response.ProductDto
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.ProductService
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 제품 검색 API Controller
 */
@RestController
@RequestMapping("/api/v1/products")
class ProductController(
    private val productService: ProductService
) {

    /**
     * 제품 검색
     * GET /api/v1/products/search
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
}
