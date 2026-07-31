package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.BranchScopeResult
import com.otoki.powersales.admin.dto.response.DashboardResponse
import com.otoki.powersales.admin.service.AdminDashboardService
import com.otoki.powersales.admin.service.UnifiedBranchScopeResolver
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
    private val unifiedBranchScopeResolver: UnifiedBranchScopeResolver
) {

    /**
     * 투입현황 대시보드 조회. 별도 권한 가드 없이 인증된(로그인한) 모든 admin 사용자 접근 가능.
     *
     * 조회 데이터 범위는 [UnifiedBranchScopeResolver] 가 사용자 권한별 지점 화이트리스트로 제한한다.
     */
    @GetMapping
    fun getDashboard(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) yearMonth: String?,
        @RequestParam(required = false) branchCode: List<String>?
    ): ResponseEntity<ApiResponse<DashboardResponse>> {
        if (yearMonth != null && !YEAR_MONTH_PATTERN.matches(yearMonth)) {
            throw InvalidYearMonthException()
        }

        // 조회 조건(지점) 라벨을 응답에 채우기 위한 코드→지점명 맵 — branches 셀렉터와 동일 산출 로직 재사용.
        val branchNamesByCode = unifiedBranchScopeResolver.resolveBranches(principal)
            .associate { it.branchCode to it.branchName }

        // 조회 지점 스코프 — 판정(요청 ⊆ 셀렉터 화이트리스트) + 확장(BranchCodeExpander)을 통합 리졸버가
        // 한 번에 산출한다. 셀렉터에 보이는 지점은 항상 조회 가능(셀렉터·판정 동일 출처).
        //  - Allowed → grantedCodes(원본, 라벨용) / queryCodes(확장, IN 필터용).
        //  - NoAccess → 매칭 0건 sentinel(빈 문자열). repository 는 "빈 목록 = 전건" 이므로
        //    빈 목록으로 넘기면 안 된다 → sentinel 로 0건 보장.
        val (grantedCodes, queryCodes) = when (
            val result = unifiedBranchScopeResolver.resolveScope(principal, branchCode)
        ) {
            is BranchScopeResult.Allowed -> result.grantedCodes to result.queryCodes
            is BranchScopeResult.NoAccess -> listOf("") to listOf("")
        }

        val response = adminDashboardService.getDashboard(grantedCodes, yearMonth, branchNamesByCode, queryCodes)
        return ResponseEntity.ok(ApiResponse.success(response, "대시보드 조회 성공"))
    }

    /**
     * 대시보드 지점 셀렉터 옵션. 대시보드와 동일하게 별도 권한 가드 없이 인증된 모든 admin 사용자 접근 가능.
     *
     * [UnifiedBranchScopeResolver] 로 산출 — 전사 권한자는 대시보드 고정 화이트리스트 34개
     * (Retail 32개 지점 + 영업지원2팀 + CVS전략팀), 그 외는 본인 조직 트리(상위 조직 사용자는 하위 지점
     * 여러 건). 이 목록이 곧 조회 판정 화이트리스트라, 셀렉터에 보이는 지점은 항상 조회된다.
     */
    @GetMapping("/branches")
    fun getBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        val result = unifiedBranchScopeResolver.resolveBranches(principal)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    companion object {
        private val YEAR_MONTH_PATTERN = Regex("^\\d{4}-(0[1-9]|1[0-2])$")
    }
}
