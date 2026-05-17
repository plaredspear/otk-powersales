package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.request.UpdateAuthorityRequest
import com.otoki.powersales.admin.dto.request.UpdateUserPermissionsRequest
import com.otoki.powersales.admin.dto.response.EmployeePermissionDetailResponse
import com.otoki.powersales.admin.dto.response.UpdateAuthorityResponse
import com.otoki.powersales.admin.service.AdminEmployeePermissionService
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/employees")
class AdminEmployeePermissionController(
    private val adminEmployeePermissionService: AdminEmployeePermissionService,
) {

    @GetMapping("/{employeeId}/permissions")
    fun getEmployeePermissions(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable employeeId: Long
    ): ResponseEntity<ApiResponse<EmployeePermissionDetailResponse>> {
        val response = adminEmployeePermissionService.getEmployeePermissions(principal, employeeId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/{employeeId}/permissions")
    fun updateUserPermissions(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable employeeId: Long,
        @Valid @RequestBody request: UpdateUserPermissionsRequest
    ): ResponseEntity<ApiResponse<EmployeePermissionDetailResponse>> {
        val response = adminEmployeePermissionService.updateUserPermissions(principal, employeeId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/{employeeId}/authority")
    fun updateAuthority(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable employeeId: Long,
        @Valid @RequestBody request: UpdateAuthorityRequest
    ): ResponseEntity<ApiResponse<UpdateAuthorityResponse>> {
        val response = adminEmployeePermissionService.updateAuthority(principal, employeeId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
