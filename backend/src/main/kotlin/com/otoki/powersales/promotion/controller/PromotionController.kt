package com.otoki.powersales.promotion.controller

import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.promotion.dto.response.MobilePromotionDetailResponse
import com.otoki.powersales.promotion.dto.response.MobilePromotionListResponse
import com.otoki.powersales.promotion.dto.response.MyPromotionAssignmentItem
import com.otoki.powersales.promotion.service.MobilePromotionService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/mobile/promotions")
class PromotionController(
    private val mobilePromotionService: MobilePromotionService
) {

    @GetMapping
    fun getPromotions(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) accountId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<MobilePromotionListResponse>> {
        val effectiveSize = size.coerceAtMost(100)
        val result = mobilePromotionService.getPromotions(
            principal.userId, startDate, endDate, keyword, accountId, page, effectiveSize
        )
        return ResponseEntity.ok(ApiResponse.success(result, "조회 성공"))
    }

    /**
     * 로그인 여사원의 담당 행사 일람 (날짜 미지정 시 오늘).
     * 홈 "행사매출 등록" → 일 매출 등록 진입화면의 "담당 행사 선택" 목록.
     */
    @GetMapping("/my-assignments")
    fun getMyAssignments(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) date: String?
    ): ResponseEntity<ApiResponse<List<MyPromotionAssignmentItem>>> {
        val result = mobilePromotionService.getMyAssignments(principal.userId, date)
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
