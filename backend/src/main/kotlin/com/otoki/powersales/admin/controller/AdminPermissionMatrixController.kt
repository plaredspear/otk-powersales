package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.request.UpdateRolePermissionsRequest
import com.otoki.powersales.admin.dto.response.PermissionMatrixResponse
import com.otoki.powersales.admin.dto.response.RolePermissionsUpdateResponse
import com.otoki.powersales.admin.scope.AdminEmployeeHolder
import com.otoki.powersales.admin.service.AdminEmployeePermissionService
import com.otoki.powersales.admin.service.AdminPermissionMatrixService
import com.otoki.powersales.auth.entity.UserRole
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
    // WebAdminContextFilter 가 요청 진입 시 산출한 Employee 를 1회 읽어 service 에 explicit
    // parameter 로 전달.
    private val adminEmployeeHolder: AdminEmployeeHolder,
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
        @PathVariable role: UserRole,
        @Valid @RequestBody request: UpdateRolePermissionsRequest
    ): ResponseEntity<ApiResponse<RolePermissionsUpdateResponse>> {
        val response = adminEmployeePermissionService.updateRolePermissions(adminEmployeeHolder.require(), role, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
