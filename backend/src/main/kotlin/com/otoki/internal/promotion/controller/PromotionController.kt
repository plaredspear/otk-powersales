package com.otoki.internal.promotion.controller

import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.promotion.dto.response.MobilePromotionDetailResponse
import com.otoki.internal.promotion.dto.response.MobilePromotionListResponse
import com.otoki.internal.promotion.service.MobilePromotionService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/promotions")
class PromotionController(
    private val mobilePromotionService: MobilePromotionService
) {

    @GetMapping
    fun getPromotions(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<MobilePromotionListResponse>> {
        val effectiveSize = size.coerceAtMost(100)
        val result = mobilePromotionService.getPromotions(
            principal.userId, startDate, endDate, keyword, page, effectiveSize
        )
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }

    @GetMapping("/{id}")
    fun getPromotion(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<MobilePromotionDetailResponse>> {
        val result = mobilePromotionService.getPromotion(principal.userId, id)
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }
}
