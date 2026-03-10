package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.response.EmployeeListResponse
import com.otoki.internal.admin.security.AdminPermission
import com.otoki.internal.admin.security.RequiresPermission
import com.otoki.internal.admin.service.AdminEmployeeService
import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.common.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/employees")
class AdminEmployeeController(
    private val adminEmployeeService: AdminEmployeeService
) {

    @GetMapping
    @RequiresPermission(AdminPermission.EMPLOYEE_READ)
    fun getEmployees(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) costCenterCode: String?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) appAuthority: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<EmployeeListResponse>> {
        val response = adminEmployeeService.getEmployees(
            status = status,
            costCenterCode = costCenterCode,
            keyword = keyword,
            appAuthority = appAuthority,
            page = page,
            size = size
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
