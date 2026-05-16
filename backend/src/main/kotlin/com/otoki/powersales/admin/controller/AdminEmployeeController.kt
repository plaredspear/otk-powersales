package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.security.RequiresPermission
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.employee.dto.response.EmployeeListResponse
import com.otoki.powersales.employee.dto.response.ResetDeviceResponse
import com.otoki.powersales.employee.dto.response.ResetPasswordResponse
import com.otoki.powersales.employee.service.AdminEmployeeCredentialService
import com.otoki.powersales.employee.service.AdminEmployeeService
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.auth.web.WebUserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/employees")
class AdminEmployeeController(
    private val adminEmployeeService: AdminEmployeeService,
    private val adminEmployeeCredentialService: AdminEmployeeCredentialService,
) {

    @GetMapping
    @RequiresPermission(AdminPermission.EMPLOYEE_READ)
    fun getEmployees(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) costCenterCode: String?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) role: UserRole?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<EmployeeListResponse>> {
        val response = adminEmployeeService.getEmployees(
            scope = scope,
            status = status,
            costCenterCode = costCenterCode,
            keyword = keyword,
            role = role,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/{employeeId}/reset-device")
    @RequiresPermission(AdminPermission.EMPLOYEE_RESET_CREDENTIALS)
    fun resetDevice(
        @PathVariable employeeId: Long
    ): ResponseEntity<ApiResponse<ResetDeviceResponse>> {
        val response = adminEmployeeCredentialService.resetDevice(employeeId)
        return ResponseEntity.ok(ApiResponse.success(response, "단말이 초기화되었습니다"))
    }

    @PostMapping("/{employeeId}/reset-password")
    @RequiresPermission(AdminPermission.EMPLOYEE_RESET_CREDENTIALS)
    fun resetPassword(
        @PathVariable employeeId: Long
    ): ResponseEntity<ApiResponse<ResetPasswordResponse>> {
        val response = adminEmployeeCredentialService.resetPassword(employeeId)
        return ResponseEntity.ok(ApiResponse.success(response, "비밀번호가 초기화되었습니다"))
    }
}
