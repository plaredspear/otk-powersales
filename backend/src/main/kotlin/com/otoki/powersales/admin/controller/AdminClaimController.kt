package com.otoki.powersales.admin.controller

import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.claim.dto.response.AdminClaimDetailResponse
import com.otoki.powersales.claim.dto.response.AdminClaimListResponse
import com.otoki.powersales.claim.service.AdminClaimService
import com.otoki.powersales.common.dto.ApiResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/admin/claims")
class AdminClaimController(
    private val adminClaimService: AdminClaimService
) {

    @GetMapping
    @RequiresSfPermission(entity = "claim", operation = SfPermissionOperation.READ)
    fun getClaims(
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: LocalDate?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) employeeName: String?,
        @RequestParam(required = false) storeName: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<AdminClaimListResponse>> {
        val response = adminClaimService.getClaims(startDate, endDate, status, employeeName, storeName, page, size)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{claimId}")
    @RequiresSfPermission(entity = "claim", operation = SfPermissionOperation.READ)
    fun getClaimDetail(
        @PathVariable claimId: Long
    ): ResponseEntity<ApiResponse<AdminClaimDetailResponse>> {
        val response = adminClaimService.getClaimDetail(claimId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
