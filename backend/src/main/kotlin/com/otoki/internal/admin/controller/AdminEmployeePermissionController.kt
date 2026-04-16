package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.request.UpdateAuthorityRequest
import com.otoki.internal.admin.dto.request.UpdateUserPermissionsRequest
import com.otoki.internal.admin.dto.response.EmployeePermissionDetailResponse
import com.otoki.internal.admin.dto.response.UpdateAuthorityResponse
import com.otoki.internal.admin.service.AdminEmployeePermissionService
import com.otoki.internal.common.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/admin/employees")
class AdminEmployeePermissionController(
    private val adminEmployeePermissionService: AdminEmployeePermissionService
) {

    @GetMapping("/{employeeId}/permissions")
    fun getEmployeePermissions(
        @PathVariable employeeId: Long
    ): ResponseEntity<ApiResponse<EmployeePermissionDetailResponse>> {
        val response = adminEmployeePermissionService.getEmployeePermissions(employeeId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/{employeeId}/permissions")
    fun updateUserPermissions(
        @PathVariable employeeId: Long,
        @Valid @RequestBody request: UpdateUserPermissionsRequest
    ): ResponseEntity<ApiResponse<EmployeePermissionDetailResponse>> {
        val response = adminEmployeePermissionService.updateUserPermissions(employeeId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PutMapping("/{employeeId}/authority")
    fun updateAuthority(
        @PathVariable employeeId: Long,
        @Valid @RequestBody request: UpdateAuthorityRequest
    ): ResponseEntity<ApiResponse<UpdateAuthorityResponse>> {
        val response = adminEmployeePermissionService.updateAuthority(employeeId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
