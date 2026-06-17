package com.otoki.powersales.domain.foundation.product.controller

import com.otoki.powersales.domain.foundation.product.dto.response.OrderProductDto
import com.otoki.powersales.domain.foundation.product.service.FavoriteProductService
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 즐겨찾기 제품 API Controller (레거시 `/prdfavorite` 정합).
 *
 * 레거시는 `favoriteProc` 단일 토글이지만, 모바일은 추가/삭제를 별도 UseCase 로 호출하므로
 * POST(추가)/DELETE(해제) 로 분리한다(이미 클라이언트가 현재 즐겨찾기 상태를 알고 있음).
 */
@RestController
@RequestMapping("/api/v1/mobile/me/favorite-products")
class FavoriteProductController(
    private val favoriteProductService: FavoriteProductService
) {

    @GetMapping
    fun getMyFavoriteProducts(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<List<OrderProductDto>>> {
        val result = favoriteProductService.getMyFavoriteProducts(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }

    @PostMapping("/{productCode}")
    fun addFavoriteProduct(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable productCode: String
    ): ResponseEntity<ApiResponse<Unit>> {
        favoriteProductService.addFavoriteProduct(principal.userId, productCode)
        return ResponseEntity.ok(ApiResponse.success(Unit, "즐겨찾기에 추가되었습니다"))
    }

    @DeleteMapping("/{productCode}")
    fun removeFavoriteProduct(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable productCode: String
    ): ResponseEntity<ApiResponse<Unit>> {
        favoriteProductService.removeFavoriteProduct(principal.userId, productCode)
        return ResponseEntity.ok(ApiResponse.success(Unit, "즐겨찾기에서 해제되었습니다"))
    }
}
