package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.response.DashboardResponse
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.service.AdminDashboardService
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.common.dto.ApiResponse
import com.otoki.powersales.common.dto.response.BranchResponse
import com.otoki.powersales.schedule.service.AdminTeamScheduleService
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
    private val adminTeamScheduleService: AdminTeamScheduleService
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
        val branchNamesByCode = adminTeamScheduleService.getBranches(principal)
            .associate { it.branchCode to it.branchName }

        val response = adminDashboardService.getDashboard(scope, yearMonth, branchCode, branchNamesByCode)
        return ResponseEntity.ok(ApiResponse.success(response, "대시보드 조회 성공"))
    }

    /**
     * 대시보드 지점 셀렉터 옵션. 대시보드와 동일하게 별도 권한 가드 없이 인증된 모든 admin 사용자 접근 가능.
     *
     * 여사원일정 화면의 `/api/v1/admin/team-schedule/branches` 와 동일 산출 로직 ([AdminTeamScheduleService.getBranches])
     * 을 재사용하되, 해당 엔드포인트는 `team_member_schedule:R` 가드가 걸려있어 대시보드 전용 가드-free endpoint 로 분리한다.
     * 반환 지점 범위는 사용자 권한 (SYSTEM_ADMIN / ALL_BRANCHES / 본인 지점) 기준으로 계속 제한된다.
     */
    @GetMapping("/branches")
    fun getBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        val result = adminTeamScheduleService.getBranches(principal)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    companion object {
        private val YEAR_MONTH_PATTERN = Regex("^\\d{4}-(0[1-9]|1[0-2])$")
    }
}
