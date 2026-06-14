package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.domain.activity.productexpiration.dto.request.AdminProductExpirationBatchDeleteRequest
import com.otoki.powersales.domain.activity.productexpiration.dto.request.AdminProductExpirationCreateRequest
import com.otoki.powersales.domain.activity.productexpiration.dto.request.AdminProductExpirationUpdateRequest
import com.otoki.powersales.domain.activity.productexpiration.dto.response.AdminProductExpirationBatchDeleteResponse
import com.otoki.powersales.domain.activity.productexpiration.dto.response.AdminProductExpirationListResponse
import com.otoki.powersales.domain.activity.productexpiration.dto.response.AdminProductExpirationResponse
import com.otoki.powersales.domain.activity.productexpiration.dto.response.AdminProductExpirationSummaryResponse
import com.otoki.powersales.domain.activity.productexpiration.service.AdminProductExpirationService
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
class AdminProductExpirationController(
    private val adminProductExpirationService: AdminProductExpirationService
) {

    @GetMapping("/api/v1/admin/product-expiration")
    @RequiresSfPermission(entity = "product", operation = SfPermissionOperation.READ)
    fun getList(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) fromDate: LocalDate?,
        @RequestParam(required = false) toDate: LocalDate?,
        @RequestParam(required = false) employeeKeyword: String?,
        @RequestParam(required = false) accountKeyword: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<AdminProductExpirationListResponse>> {
        val effectiveSize = size.coerceAtMost(100)
        val response = adminProductExpirationService.getList(
            principal.requireEmployeeId(), fromDate, toDate, employeeKeyword, accountKeyword, status,
            PageRequest.of(page, effectiveSize)
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/api/v1/admin/product-expiration/{id}")
    @RequiresSfPermission(entity = "product", operation = SfPermissionOperation.READ)
    fun getDetail(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Int
    ): ResponseEntity<ApiResponse<AdminProductExpirationResponse>> {
        val response = adminProductExpirationService.getDetail(principal.requireEmployeeId(), id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/product-expiration")
    @RequiresSfPermission(entity = "product", operation = SfPermissionOperation.EDIT)
    fun create(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: AdminProductExpirationCreateRequest
    ): ResponseEntity<ApiResponse<AdminProductExpirationResponse>> {
        val response = adminProductExpirationService.create(principal.requireEmployeeId(), request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/api/v1/admin/product-expiration/{id}")
    @RequiresSfPermission(entity = "product", operation = SfPermissionOperation.EDIT)
    fun update(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Int,
        @Valid @RequestBody request: AdminProductExpirationUpdateRequest
    ): ResponseEntity<ApiResponse<AdminProductExpirationResponse>> {
        val response = adminProductExpirationService.update(principal.requireEmployeeId(), id, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/api/v1/admin/product-expiration/{id}")
    @RequiresSfPermission(entity = "product", operation = SfPermissionOperation.EDIT)
    fun delete(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Int
    ): ResponseEntity<ApiResponse<Any?>> {
        adminProductExpirationService.delete(principal.requireEmployeeId(), id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?))
    }

    @PostMapping("/api/v1/admin/product-expiration/batch-delete")
    @RequiresSfPermission(entity = "product", operation = SfPermissionOperation.EDIT)
    fun batchDelete(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: AdminProductExpirationBatchDeleteRequest
    ): ResponseEntity<ApiResponse<AdminProductExpirationBatchDeleteResponse>> {
        val response = adminProductExpirationService.batchDelete(principal.requireEmployeeId(), request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/api/v1/admin/product-expiration/summary")
    @RequiresSfPermission(entity = "product", operation = SfPermissionOperation.READ)
    fun getSummary(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ApiResponse<AdminProductExpirationSummaryResponse>> {
        val response = adminProductExpirationService.getSummary(principal.requireEmployeeId())
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
