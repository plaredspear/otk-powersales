package com.otoki.internal.controller

import com.otoki.internal.dto.ApiResponse
import com.otoki.internal.dto.response.FavoriteProductResponse
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.FavoriteProductService
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 즐겨찾기 제품 API Controller
 */
@RestController
@RequestMapping("/api/v1/me/favorite-products")
class FavoriteProductController(
    private val favoriteProductService: FavoriteProductService
) {

    /**
     * 즐겨찾기 제품 목록 조회
     * GET /api/v1/me/favorite-products
     */
    @GetMapping
    fun getMyFavoriteProducts(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?
    ): ResponseEntity<ApiResponse<Page<FavoriteProductResponse>>> {
        val result = favoriteProductService.getMyFavoriteProducts(
            userId = principal.userId,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }

    /**
     * 즐겨찾기 추가
     * POST /api/v1/me/favorite-products/{productCode}
     */
    @PostMapping("/{productCode}")
    fun addFavoriteProduct(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable productCode: String
    ): ResponseEntity<ApiResponse<Any?>> {
        favoriteProductService.addFavoriteProduct(
            userId = principal.userId,
            productCode = productCode
        )
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "즐겨찾기에 추가되었습니다"))
    }

    /**
     * 즐겨찾기 해제
     * DELETE /api/v1/me/favorite-products/{productCode}
     */
    @DeleteMapping("/{productCode}")
    fun removeFavoriteProduct(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable productCode: String
    ): ResponseEntity<ApiResponse<Any?>> {
        favoriteProductService.removeFavoriteProduct(
            userId = principal.userId,
            productCode = productCode
        )
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "즐겨찾기에서 해제되었습니다"))
    }
}
