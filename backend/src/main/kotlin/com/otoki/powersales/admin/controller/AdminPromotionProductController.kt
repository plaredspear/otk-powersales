package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.promotion.dto.request.PromotionPosProductRequest
import com.otoki.powersales.promotion.dto.response.PromotionPosProductResponse
import com.otoki.powersales.promotion.service.AdminPromotionService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 상세 POS품목 (DKRetail__PromotionProduct__c) 단건 수정/삭제 — SF Lightning 의 row dropdown
 * (편집 / 삭제) 동등.
 *
 * 생성은 부모 행사 컨텍스트가 필요하므로 [AdminPromotionController.createPosProduct] 에서
 * `/promotions/{id}/pos-products` 로 처리. 본 controller 는 PromotionEmployee 패턴과 동일하게
 * id 단위 PUT/DELETE 만 담당.
 */
@RestController
@RequestMapping("/api/v1/admin/promotion-products")
class AdminPromotionProductController(
    private val adminPromotionService: AdminPromotionService,
) {

    @PutMapping("/{id}")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun update(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long,
        @Valid @RequestBody request: PromotionPosProductRequest,
    ): ResponseEntity<ApiResponse<PromotionPosProductResponse>> {
        val response = adminPromotionService.updatePosProduct(scope, id, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/{id}")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun delete(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<Any?>> {
        adminPromotionService.deletePosProduct(scope, id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?))
    }
}
