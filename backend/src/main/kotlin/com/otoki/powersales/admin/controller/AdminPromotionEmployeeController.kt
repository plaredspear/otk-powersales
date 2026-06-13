package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.promotion.dto.request.BatchUpdatePromotionEmployeeRequest
import com.otoki.powersales.promotion.dto.request.PromotionEmployeeRequest
import com.otoki.powersales.promotion.dto.response.BatchUpdatePromotionEmployeeResponse
import com.otoki.powersales.promotion.dto.response.PromotionConfirmResponse
import com.otoki.powersales.promotion.dto.response.PromotionEmployeeDetailResponse
import com.otoki.powersales.promotion.dto.response.PromotionEmployeeListResponse
import com.otoki.powersales.promotion.service.AdminPromotionConfirmService
import com.otoki.powersales.promotion.service.AdminPromotionEmployeeService
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
class AdminPromotionEmployeeController(
    private val adminPromotionEmployeeService: AdminPromotionEmployeeService,
    private val adminPromotionConfirmService: AdminPromotionConfirmService
) {

    @GetMapping("/api/v1/admin/promotions/{promotionId}/employees")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun getEmployees(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable promotionId: Long
    ): ResponseEntity<ApiResponse<List<PromotionEmployeeListResponse>>> {
        val response = adminPromotionEmployeeService.getEmployees(scope, promotionId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/api/v1/admin/promotion-employees/{id}")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.READ)
    fun getEmployee(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<PromotionEmployeeDetailResponse>> {
        val response = adminPromotionEmployeeService.getEmployee(id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/promotions/{promotionId}/employees")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun createEmployee(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable promotionId: Long,
        @Valid @RequestBody request: PromotionEmployeeRequest
    ): ResponseEntity<ApiResponse<PromotionEmployeeDetailResponse>> {
        val response = adminPromotionEmployeeService.createEmployee(promotionId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/api/v1/admin/promotions/{promotionId}/employees/batch")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun batchUpdateEmployees(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable promotionId: Long,
        @Valid @RequestBody request: BatchUpdatePromotionEmployeeRequest
    ): ResponseEntity<ApiResponse<BatchUpdatePromotionEmployeeResponse>> {
        val response = adminPromotionEmployeeService.batchUpdateEmployees(principal, promotionId, principal.requireEmployeeId(), request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/api/v1/admin/promotion-employees/{id}")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun updateEmployee(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: PromotionEmployeeRequest
    ): ResponseEntity<ApiResponse<PromotionEmployeeDetailResponse>> {
        val response = adminPromotionEmployeeService.updateEmployee(principal, id, principal.requireEmployeeId(), request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/api/v1/admin/promotion-employees/{id}")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun deleteEmployee(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        adminPromotionEmployeeService.deleteEmployee(principal, id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?))
    }

    @PostMapping("/api/v1/admin/promotions/{promotionId}/confirm")
    @RequiresSfPermission(entity = "promotion", operation = SfPermissionOperation.EDIT)
    fun confirmPromotion(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable promotionId: Long
    ): ResponseEntity<ApiResponse<PromotionConfirmResponse>> {
        val response = adminPromotionConfirmService.confirmPromotion(promotionId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
