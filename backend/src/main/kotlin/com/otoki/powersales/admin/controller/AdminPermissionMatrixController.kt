package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.request.UpdateRolePermissionsRequest
import com.otoki.powersales.admin.dto.response.PermissionMatrixResponse
import com.otoki.powersales.admin.dto.response.RolePermissionsUpdateResponse
import com.otoki.powersales.admin.service.AdminEmployeePermissionService
import com.otoki.powersales.admin.service.AdminPermissionMatrixService
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/permissions")
class AdminPermissionMatrixController(
    private val adminPermissionMatrixService: AdminPermissionMatrixService,
    private val adminEmployeePermissionService: AdminEmployeePermissionService
) {

    @GetMapping("/matrix")
    fun getMatrix(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<PermissionMatrixResponse>> {
        val response = adminPermissionMatrixService.getMatrix(principal.userId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/roles/{role}")
    fun updateRolePermissions(
        @PathVariable role: String,
        @Valid @RequestBody request: UpdateRolePermissionsRequest
    ): ResponseEntity<ApiResponse<RolePermissionsUpdateResponse>> {
        val response = adminEmployeePermissionService.updateRolePermissions(role, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
