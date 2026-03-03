package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.response.DashboardResponse
import com.otoki.internal.admin.service.AdminDashboardService
import com.otoki.internal.common.dto.ApiResponse
import com.otoki.internal.common.exception.BusinessException

import com.otoki.internal.common.security.UserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/dashboard")
class AdminDashboardController(
    private val adminDashboardService: AdminDashboardService
) {

    @GetMapping
    fun getDashboard(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(required = false) yearMonth: String?,
        @RequestParam(required = false) branchCode: String?
    ): ResponseEntity<ApiResponse<DashboardResponse>> {
        if (yearMonth != null && !YEAR_MONTH_PATTERN.matches(yearMonth)) {
            throw InvalidYearMonthException()
        }

        val response = adminDashboardService.getDashboard(principal.userId, yearMonth, branchCode)
        return ResponseEntity.ok(ApiResponse.success(response, "대시보드 조회 성공"))
    }

    companion object {
        private val YEAR_MONTH_PATTERN = Regex("^\\d{4}-(0[1-9]|1[0-2])$")
    }
}

class InvalidYearMonthException : BusinessException(
    errorCode = "VALIDATION_ERROR",
    message = "유효하지 않은 yearMonth 형식입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
