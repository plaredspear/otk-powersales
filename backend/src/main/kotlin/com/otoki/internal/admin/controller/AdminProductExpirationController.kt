package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.request.AdminProductExpirationBatchDeleteRequest
import com.otoki.internal.admin.dto.request.AdminProductExpirationCreateRequest
import com.otoki.internal.admin.dto.request.AdminProductExpirationUpdateRequest
import com.otoki.internal.admin.dto.response.AdminProductExpirationBatchDeleteResponse
import com.otoki.internal.admin.dto.response.AdminProductExpirationListResponse
import com.otoki.internal.admin.dto.response.AdminProductExpirationResponse
import com.otoki.internal.admin.dto.response.AdminProductExpirationSummaryResponse
import com.otoki.internal.admin.security.AdminPermission
import com.otoki.internal.admin.security.RequiresPermission
import com.otoki.internal.admin.service.AdminProductExpirationService
import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.common.security.UserPrincipal
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
    @RequiresPermission(AdminPermission.PRODUCT_EXPIRATION_READ)
    fun getList(
        @AuthenticationPrincipal principal: UserPrincipal,
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
            principal.userId, fromDate, toDate, employeeKeyword, accountKeyword, status,
            PageRequest.of(page, effectiveSize)
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/api/v1/admin/product-expiration/{id}")
    @RequiresPermission(AdminPermission.PRODUCT_EXPIRATION_READ)
    fun getDetail(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Int
    ): ResponseEntity<ApiResponse<AdminProductExpirationResponse>> {
        val response = adminProductExpirationService.getDetail(principal.userId, id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/product-expiration")
    @RequiresPermission(AdminPermission.PRODUCT_EXPIRATION_WRITE)
    fun create(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: AdminProductExpirationCreateRequest
    ): ResponseEntity<ApiResponse<AdminProductExpirationResponse>> {
        val response = adminProductExpirationService.create(principal.userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/api/v1/admin/product-expiration/{id}")
    @RequiresPermission(AdminPermission.PRODUCT_EXPIRATION_WRITE)
    fun update(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Int,
        @Valid @RequestBody request: AdminProductExpirationUpdateRequest
    ): ResponseEntity<ApiResponse<AdminProductExpirationResponse>> {
        val response = adminProductExpirationService.update(principal.userId, id, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/api/v1/admin/product-expiration/{id}")
    @RequiresPermission(AdminPermission.PRODUCT_EXPIRATION_WRITE)
    fun delete(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable id: Int
    ): ResponseEntity<ApiResponse<Any?>> {
        adminProductExpirationService.delete(principal.userId, id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?))
    }

    @PostMapping("/api/v1/admin/product-expiration/batch-delete")
    @RequiresPermission(AdminPermission.PRODUCT_EXPIRATION_WRITE)
    fun batchDelete(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: AdminProductExpirationBatchDeleteRequest
    ): ResponseEntity<ApiResponse<AdminProductExpirationBatchDeleteResponse>> {
        val response = adminProductExpirationService.batchDelete(principal.userId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/api/v1/admin/product-expiration/summary")
    @RequiresPermission(AdminPermission.PRODUCT_EXPIRATION_READ)
    fun getSummary(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<AdminProductExpirationSummaryResponse>> {
        val response = adminProductExpirationService.getSummary(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
