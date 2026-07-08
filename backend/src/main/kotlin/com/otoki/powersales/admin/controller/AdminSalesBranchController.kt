package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.service.DashboardBranchResolver
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 매출/실적 계열 화면(월 매출 물류배부·전산실적·POS·월별 투입적합성·배치 적합성) 지점 셀렉터 endpoint.
 *
 * **화면별 전용 endpoint** 로 분리한다 — 화면마다 지점 셀렉터 권한/스코프를 독립적으로 조정할 수 있게
 * 하기 위함. 현재는 모두 `monthly_sales_history` READ 로 게이팅되지만, 향후 특정 화면만 권한 entity
 * 를 세분화하거나 지점 스코프를 달리해야 할 때 다른 화면에 영향 없이 해당 핸들러만 바꾼다.
 *
 * 여사원 일정관리의 `/team-schedule/branches` (`team_member_schedule` 가드) 를 빌려쓰면
 * `team_member_schedule` READ 없는 사용자가 지점 셀렉터에서 403 이 나므로 매출/실적 계열은
 * `monthly_sales_history` 가드의 이 컨트롤러로 분리한다.
 *
 * 지점 목록 산출:
 * - 월 매출(물류배부)만 대시보드와 동일한 고정 화이트리스트(34개, [DashboardBranchResolver]).
 * - 나머지 4개 화면은 여사원 일정/현황/전문행사조와 동일 단일 출처([WomenScheduleBranchResolver],
 *   사용자 권한 기준 조직 트리 스코프) 를 재사용한다.
 */
@RestController
@RequestMapping("/api/v1/admin/sales")
class AdminSalesBranchController(
    private val womenScheduleBranchResolver: WomenScheduleBranchResolver,
    private val dashboardBranchResolver: DashboardBranchResolver,
) {

    /** 월 매출(전산실적) 전용 지점 셀렉터 옵션 — 조직 트리 스코프([WomenScheduleBranchResolver]). */
    @GetMapping("/electronic/branches")
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    fun getElectronicSalesBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> =
        ResponseEntity.ok(ApiResponse.success(womenScheduleBranchResolver.resolveBranches(principal)))

    /** POS매출 전용 지점 셀렉터 옵션 — 조직 트리 스코프([WomenScheduleBranchResolver]). */
    @GetMapping("/pos/branches")
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    fun getPosSalesBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> =
        ResponseEntity.ok(ApiResponse.success(womenScheduleBranchResolver.resolveBranches(principal)))

    /** 월별 진열사원 투입적합성 전용 지점 셀렉터 옵션 — 조직 트리 스코프([WomenScheduleBranchResolver]). */
    @GetMapping("/input-adequacy/branches")
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    fun getInputAdequacyBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> =
        ResponseEntity.ok(ApiResponse.success(womenScheduleBranchResolver.resolveBranches(principal)))

    /** 진열사원 배치 적합성 전용 지점 셀렉터 옵션 — 조직 트리 스코프([WomenScheduleBranchResolver]). */
    @GetMapping("/deployment/branches")
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    fun getDeploymentBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> =
        ResponseEntity.ok(ApiResponse.success(womenScheduleBranchResolver.resolveBranches(principal)))

    /**
     * 월 매출(물류배부) 전용 지점 셀렉터 옵션.
     *
     * 다른 매출/실적 화면이 여사원 일정 스코프([WomenScheduleBranchResolver], 조직 트리 전체) 를
     * 반환하는 것과 달리, 월 매출(물류배부) 화면은 대시보드와 동일한 지점 기준을 요구한다.
     * 따라서 전사 권한자에게 대시보드 고정 화이트리스트(34개) 를 노출하는 [DashboardBranchResolver] 를
     * 재사용한다.
     *
     * 셀렉터 목록만 34개로 좁히고 실제 조회/집계 스코프는 기존과 동일하다(셀렉터에서 고른 값은
     * 조직 트리 스코프의 부분집합).
     */
    @GetMapping("/monthly/branches")
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    fun getMonthlySalesBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> =
        ResponseEntity.ok(ApiResponse.success(dashboardBranchResolver.resolveBranches(principal)))
}
