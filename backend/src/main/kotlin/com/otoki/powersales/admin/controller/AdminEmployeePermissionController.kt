package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.request.UpdateAuthorityRequest
import com.otoki.powersales.admin.dto.request.UpdateUserPermissionsRequest
import com.otoki.powersales.admin.dto.response.EmployeePermissionDetailResponse
import com.otoki.powersales.admin.dto.response.UpdateAuthorityResponse
import com.otoki.powersales.admin.scope.AdminEmployeeHolder
import com.otoki.powersales.admin.service.AdminEmployeePermissionService
import com.otoki.powersales.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/employees")
class AdminEmployeePermissionController(
    private val adminEmployeePermissionService: AdminEmployeePermissionService,
    // WebAdminContextFilter 가 요청 진입 시 산출한 Employee 를 1회 읽어 service 에 explicit
    // parameter 로 전달.
    private val adminEmployeeHolder: AdminEmployeeHolder,
) {

    @GetMapping("/{employeeId}/permissions")
    fun getEmployeePermissions(
        @PathVariable employeeId: Long
    ): ResponseEntity<ApiResponse<EmployeePermissionDetailResponse>> {
        val response = adminEmployeePermissionService.getEmployeePermissions(adminEmployeeHolder.require(), employeeId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/{employeeId}/permissions")
    fun updateUserPermissions(
        @PathVariable employeeId: Long,
        @Valid @RequestBody request: UpdateUserPermissionsRequest
    ): ResponseEntity<ApiResponse<EmployeePermissionDetailResponse>> {
        val response = adminEmployeePermissionService.updateUserPermissions(adminEmployeeHolder.require(), employeeId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/{employeeId}/authority")
    fun updateAuthority(
        @PathVariable employeeId: Long,
        @Valid @RequestBody request: UpdateAuthorityRequest
    ): ResponseEntity<ApiResponse<UpdateAuthorityResponse>> {
        val response = adminEmployeePermissionService.updateAuthority(adminEmployeeHolder.require(), employeeId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
