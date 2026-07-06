package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.response.DashboardResponse
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.service.AdminDashboardService
import com.otoki.powersales.admin.service.DashboardBranchResolver
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.admin.exception.InvalidYearMonthException

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin/dashboard")
class AdminDashboardController(
    private val adminDashboardService: AdminDashboardService,
    private val dashboardBranchResolver: DashboardBranchResolver
) {

    /**
     * 투입현황 대시보드 조회. 별도 권한 가드 없이 인증된(로그인한) 모든 admin 사용자 접근 가능.
     *
     * 조회 데이터 범위는 [CurrentDataScope] 가 사용자 권한 (VIEW_ALL_DATA / 지점 스코프) 기준으로 제한한다.
     */
    @GetMapping
    fun getDashboard(
        @CurrentDataScope scope: DataScope,
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) yearMonth: String?,
        @RequestParam(required = false) branchCode: String?
    ): ResponseEntity<ApiResponse<DashboardResponse>> {
        if (yearMonth != null && !YEAR_MONTH_PATTERN.matches(yearMonth)) {
            throw InvalidYearMonthException()
        }

        // 조회 조건(지점) 라벨을 응답에 채우기 위한 코드→지점명 맵 — branches 셀렉터와 동일 산출 로직 재사용.
        val branchNamesByCode = dashboardBranchResolver.resolveBranches(principal)
            .associate { it.branchCode to it.branchName }

        val response = adminDashboardService.getDashboard(scope, yearMonth, branchCode, branchNamesByCode)
        return ResponseEntity.ok(ApiResponse.success(response, "대시보드 조회 성공"))
    }

    /**
     * 대시보드 지점 셀렉터 옵션. 대시보드와 동일하게 별도 권한 가드 없이 인증된 모든 admin 사용자 접근 가능.
     *
     * [DashboardBranchResolver] 로 산출 — 전사 권한자 (SYSTEM_ADMIN / ALL_BRANCHES) 는 대시보드 전용
     * 고정 화이트리스트 (Retail 32개 지점 + 영업지원2팀 + CVS전략팀 = 34개), 그 외는 본인 지점 스코프.
     * 여사원일정 등과 공유하는 [WomenScheduleBranchResolver] 와 별개로, 대시보드에만 적용되는 규칙이다.
     */
    @GetMapping("/branches")
    fun getBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        val result = dashboardBranchResolver.resolveBranches(principal)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    companion object {
        private val YEAR_MONTH_PATTERN = Regex("^\\d{4}-(0[1-9]|1[0-2])$")
    }
}
