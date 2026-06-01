package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.response.DashboardResponse
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.service.AdminDashboardService
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.admin.exception.InvalidYearMonthException

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/dashboard")
class AdminDashboardController(
    private val adminDashboardService: AdminDashboardService
) {

    /**
     * 투입현황 대시보드 조회. 별도 권한 가드 없이 인증된(로그인한) 모든 admin 사용자 접근 가능.
     *
     * 조회 데이터 범위는 [CurrentDataScope] 가 사용자 권한 (VIEW_ALL_DATA / 지점 스코프) 기준으로 제한한다.
     */
    @GetMapping
    fun getDashboard(
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) yearMonth: String?,
        @RequestParam(required = false) branchCode: String?
    ): ResponseEntity<ApiResponse<DashboardResponse>> {
        if (yearMonth != null && !YEAR_MONTH_PATTERN.matches(yearMonth)) {
            throw InvalidYearMonthException()
        }

        val response = adminDashboardService.getDashboard(scope, yearMonth, branchCode)
        return ResponseEntity.ok(ApiResponse.success(response, "대시보드 조회 성공"))
    }

    companion object {
        private val YEAR_MONTH_PATTERN = Regex("^\\d{4}-(0[1-9]|1[0-2])$")
    }
}
