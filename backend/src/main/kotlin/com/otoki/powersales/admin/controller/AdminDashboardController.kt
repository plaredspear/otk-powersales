package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.response.DashboardResponse
import com.otoki.powersales.admin.service.AdminDashboardService
import com.otoki.powersales.auth.permission.PermissionResource
import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.admin.exception.InvalidYearMonthException

import com.otoki.powersales.auth.web.WebUserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PermissionResource("dashboard")
class AdminDashboardController(
    private val adminDashboardService: AdminDashboardService
) {

    @GetMapping
    @RequiresSfPermission(entity = "dashboard", operation = SfPermissionOperation.READ)
    fun getDashboard(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) yearMonth: String?,
        @RequestParam(required = false) branchCode: String?
    ): ResponseEntity<ApiResponse<DashboardResponse>> {
        if (yearMonth != null && !YEAR_MONTH_PATTERN.matches(yearMonth)) {
            throw InvalidYearMonthException()
        }

        val response = adminDashboardService.getDashboard(yearMonth, branchCode)
        return ResponseEntity.ok(ApiResponse.success(response, "대시보드 조회 성공"))
    }

    companion object {
        private val YEAR_MONTH_PATTERN = Regex("^\\d{4}-(0[1-9]|1[0-2])$")
    }
}
