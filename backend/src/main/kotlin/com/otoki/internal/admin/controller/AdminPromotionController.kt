package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.request.PromotionCreateRequest
import com.otoki.internal.admin.dto.response.PromotionDetailResponse
import com.otoki.internal.admin.dto.response.PromotionFormMetaResponse
import com.otoki.internal.admin.dto.response.PromotionListResponse
import com.otoki.internal.admin.security.AdminPermission
import com.otoki.internal.admin.security.RequiresPermission
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

    @GetMapping("/form-meta")
    @RequiresPermission(AdminPermission.PROMOTION_READ)
    fun getPromotionFormMeta(): ResponseEntity<ApiResponse<PromotionFormMetaResponse>> {
        val response = adminPromotionService.getPromotionFormMeta()
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping
    @RequiresPermission(AdminPermission.PROMOTION_READ)
    fun getPromotions(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) @Size(min = 1, max = 100) keyword: String?,
        @RequestParam(required = false) promotionTypeId: Long?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ResponseEntity<ApiResponse<PromotionListResponse>> {
        val response = adminPromotionService.getPromotions(
            keyword = keyword,
            promotionTypeId = promotionTypeId,
            category = category,
            startDate = startDate,
            endDate = endDate,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{id}")
    @RequiresPermission(AdminPermission.PROMOTION_READ)
    fun getPromotion(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<PromotionDetailResponse>> {
        val response = adminPromotionService.getPromotion(id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun createPromotion(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: PromotionCreateRequest
    ): ResponseEntity<ApiResponse<PromotionDetailResponse>> {
        val response = adminPromotionService.createPromotion(principal.userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/{id}")
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun updatePromotion(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: PromotionCreateRequest
    ): ResponseEntity<ApiResponse<PromotionDetailResponse>> {
        val response = adminPromotionService.updatePromotion(id, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun deletePromotion(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        adminPromotionService.deletePromotion(id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?))
    }
}
