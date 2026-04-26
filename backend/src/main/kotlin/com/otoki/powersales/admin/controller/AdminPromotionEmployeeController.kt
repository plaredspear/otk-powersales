package com.otoki.powersales.admin.controller

import com.otoki.powersales.promotion.dto.request.BatchUpdatePromotionEmployeeRequest
import com.otoki.powersales.promotion.dto.request.PromotionEmployeeRequest
import com.otoki.powersales.promotion.dto.response.BatchUpdatePromotionEmployeeResponse
import com.otoki.powersales.promotion.dto.response.PromotionConfirmResponse
import com.otoki.powersales.promotion.dto.response.PromotionEmployeeDetailResponse
import com.otoki.powersales.promotion.dto.response.PromotionEmployeeListResponse
import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.admin.security.RequiresPermission
import com.otoki.powersales.promotion.service.AdminPromotionConfirmService
import com.otoki.powersales.promotion.service.AdminPromotionEmployeeService
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.security.UserPrincipal
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
    @RequiresPermission(AdminPermission.PROMOTION_READ)
    fun getEmployees(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable promotionId: Long
    ): ResponseEntity<ApiResponse<List<PromotionEmployeeListResponse>>> {
        val response = adminPromotionEmployeeService.getEmployees(promotionId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/api/v1/admin/promotion-employees/{id}")
    @RequiresPermission(AdminPermission.PROMOTION_READ)
    fun getEmployee(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<PromotionEmployeeDetailResponse>> {
        val response = adminPromotionEmployeeService.getEmployee(id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/promotions/{promotionId}/employees")
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun createEmployee(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable promotionId: Long,
        @Valid @RequestBody request: PromotionEmployeeRequest
    ): ResponseEntity<ApiResponse<PromotionEmployeeDetailResponse>> {
        val response = adminPromotionEmployeeService.createEmployee(promotionId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/api/v1/admin/promotions/{promotionId}/employees/batch")
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun batchUpdateEmployees(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable promotionId: Long,
        @Valid @RequestBody request: BatchUpdatePromotionEmployeeRequest
    ): ResponseEntity<ApiResponse<BatchUpdatePromotionEmployeeResponse>> {
        val response = adminPromotionEmployeeService.batchUpdateEmployees(promotionId, principal.userId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/api/v1/admin/promotion-employees/{id}")
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun updateEmployee(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: PromotionEmployeeRequest
    ): ResponseEntity<ApiResponse<PromotionEmployeeDetailResponse>> {
        val response = adminPromotionEmployeeService.updateEmployee(id, principal.userId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/api/v1/admin/promotion-employees/{id}")
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun deleteEmployee(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        adminPromotionEmployeeService.deleteEmployee(id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?))
    }

    @PostMapping("/api/v1/admin/promotions/{promotionId}/confirm")
    @RequiresPermission(AdminPermission.PROMOTION_WRITE)
    fun confirmPromotion(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable promotionId: Long
    ): ResponseEntity<ApiResponse<PromotionConfirmResponse>> {
        val response = adminPromotionConfirmService.confirmPromotion(promotionId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
