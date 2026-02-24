package com.otoki.internal.product.controller

/* --- 전체 주석 처리: V1 Entity 리매핑 (Spec 77) ---
 * FavoriteProductService 비활성화에 따라 Controller도 주석 처리.

import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.product.dto.response.FavoriteProductResponse
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.product.service.FavoriteProductService
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/me/favorite-products")
class FavoriteProductController(
    private val favoriteProductService: FavoriteProductService
) {

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

--- */
