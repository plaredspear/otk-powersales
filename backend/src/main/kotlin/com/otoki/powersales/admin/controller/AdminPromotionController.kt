package com.otoki.powersales.admin.controller

import com.otoki.powersales.promotion.dto.request.PromotionCreateRequest
import com.otoki.powersales.promotion.dto.response.PromotionDetailResponse
import com.otoki.powersales.promotion.dto.response.PromotionFormMetaResponse
import com.otoki.powersales.promotion.dto.response.PromotionListResponse
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.security.RequiresPermission
import com.otoki.powersales.promotion.service.AdminPromotionService
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.auth.web.WebUserPrincipal
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
    private val adminPromotionService: AdminPromotionService,
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
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) @Size(min = 1, max = 100) keyword: String?,
        @RequestParam(required = false) promotionType: String?,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        @RequestParam(required = false, defaultValue = "0") @Min(0) page: Int,
        @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) size: Int
    ): ResponseEntity<ApiResponse<PromotionListResponse>> {
        val response = adminPromotionService.getPromotions(
            scope = scope,
            keyword = keyword,
            promotionType = promotionType,
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
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<PromotionDetailResponse>> {
        val response = adminPromotionService.getPromotion(scope, id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun createPromotion(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: PromotionCreateRequest
    ): ResponseEntity<ApiResponse<PromotionDetailResponse>> {
        val response = adminPromotionService.createPromotion(principal.requireEmployeeId(), request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/{id}")
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun updatePromotion(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long,
        @Valid @RequestBody request: PromotionCreateRequest
    ): ResponseEntity<ApiResponse<PromotionDetailResponse>> {
        val response = adminPromotionService.updatePromotion(scope, id, principal.requireEmployeeId(), request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun deletePromotion(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        adminPromotionService.deletePromotion(scope, id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?))
    }

    // UC-11: 행사마스터 복제 (폼 방식)
    // 레거시 PromotionCloneComponent Quick Action 동등.
    @PostMapping("/{id}/clone")
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun clonePromotion(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: PromotionCreateRequest
    ): ResponseEntity<ApiResponse<PromotionDetailResponse>> {
        val response = adminPromotionService.clonePromotion(id, principal.requireEmployeeId(), request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    // UC-12: 행사마스터 자식 포함 복제 (1클릭)
    // 레거시 ClonePromotionWithChildsController Quick Action 동등. body 없음.
    @PostMapping("/{id}/clone-with-children")
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun cloneWithChildren(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<PromotionDetailResponse>> {
        val response = adminPromotionService.cloneWithChildren(id, principal.requireEmployeeId())
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }
}
