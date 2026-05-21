package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.request.UpdateRolePermissionsRequest
import com.otoki.powersales.admin.dto.response.PermissionMatrixResponse
import com.otoki.powersales.admin.dto.response.RolePermissionsUpdateResponse
import com.otoki.powersales.admin.service.AdminEmployeePermissionService
import com.otoki.powersales.admin.service.AdminPermissionMatrixService
import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.auth.web.WebUserPrincipal
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/permissions")
class AdminPermissionMatrixController(
    private val adminPermissionMatrixService: AdminPermissionMatrixService,
    private val adminEmployeePermissionService: AdminEmployeePermissionService,
) {

    @GetMapping("/matrix")
    fun getMatrix(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ApiResponse<PermissionMatrixResponse>> {
        val response = adminPermissionMatrixService.getMatrix(principal.requireEmployeeId())
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/roles/{role}")
    fun updateRolePermissions(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable role: UserRoleEnum,
        @Valid @RequestBody request: UpdateRolePermissionsRequest
    ): ResponseEntity<ApiResponse<RolePermissionsUpdateResponse>> {
        val response = adminEmployeePermissionService.updateRolePermissions(principal, role, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
