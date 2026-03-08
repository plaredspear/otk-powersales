package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.request.PromotionCreateRequest
import com.otoki.internal.admin.dto.response.PromotionDetailResponse
import com.otoki.internal.admin.dto.response.PromotionListResponse
import com.otoki.internal.admin.service.AdminPromotionService
import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.common.security.UserPrincipal
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/promotions")
@Validated
class AdminPromotionController(
    private val adminPromotionService: AdminPromotionService
) {

    @GetMapping
    fun getPromotions(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) @Size(min = 1, max = 100) keyword: String?,
        @RequestParam(required = false) promotionType: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ResponseEntity<ApiResponse<PromotionListResponse>> {
        val response = adminPromotionService.getPromotions(
            userId = principal.userId,
            keyword = keyword,
            promotionType = promotionType,
            category = category,
            startDate = startDate,
            endDate = endDate,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{id}")
    fun getPromotion(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<PromotionDetailResponse>> {
        val response = adminPromotionService.getPromotion(principal.userId, id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping
    fun createPromotion(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: PromotionCreateRequest
    ): ResponseEntity<ApiResponse<PromotionDetailResponse>> {
        val response = adminPromotionService.createPromotion(principal.userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/{id}")
    fun updatePromotion(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: PromotionCreateRequest
    ): ResponseEntity<ApiResponse<PromotionDetailResponse>> {
        val response = adminPromotionService.updatePromotion(principal.userId, id, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/{id}")
    fun deletePromotion(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        adminPromotionService.deletePromotion(principal.userId, id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?))
    }
}
