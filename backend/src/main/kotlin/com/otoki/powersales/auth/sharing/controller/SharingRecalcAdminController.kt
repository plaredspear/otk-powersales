package com.otoki.powersales.auth.sharing.controller

import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.auth.permission.SfSystemPermission
import com.otoki.powersales.auth.sharing.service.SharingRecalcService
import com.otoki.powersales.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Sharing Recalc admin endpoint (spec #792).
 *
 * 권한: SYSTEM_ADMIN (= `SF_MIGRATION_RUN` permission 재사용).
 *
 * SF Sharing Recalculation 동등 — 단 본 spec 은 cache evict 만 (메타 일괄 재적재 후 admin 호출).
 * 데이터 재계산 자체는 #786 evaluator 가 매 read 시점 runtime 처리.
 */
@RestController
class SharingRecalcAdminController(
    private val service: SharingRecalcService,
) {

    @PostMapping("/api/v1/admin/sharing/recalc/all")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun recalcAll(
        @AuthenticationPrincipal principal: UserDetails?,
    ): ResponseEntity<ApiResponse<SharingRecalcService.RecalcResult>> {
        val userId = principal?.username?.toLongOrNull() ?: 0L
        val result = service.recalcAll(userId)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @PostMapping("/api/v1/admin/sharing/recalc/sobject/{sObjectName}")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun recalcSObject(
        @PathVariable sObjectName: String,
        @AuthenticationPrincipal principal: UserDetails?,
    ): ResponseEntity<ApiResponse<SharingRecalcService.RecalcResult>> {
        val userId = principal?.username?.toLongOrNull() ?: 0L
        val result = service.recalcSObject(sObjectName, userId)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping("/api/v1/admin/sharing/recalc/status")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun getStatus(): ResponseEntity<ApiResponse<SharingRecalcService.RecalcStatus>> {
        val status = service.getStatus()
        return ResponseEntity.ok(ApiResponse.success(status))
    }
}
