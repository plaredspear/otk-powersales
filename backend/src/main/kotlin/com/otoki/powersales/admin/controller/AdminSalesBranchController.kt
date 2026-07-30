package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.service.DashboardBranchResolver
import com.otoki.powersales.admin.service.WhitelistBranchScopeResolver
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
 * 하기 위함. 그 분리가 실제로 쓰인 사례가 아래 가드 이원화다:
 *
 * - 대시보드 3화면(물류배부 / 전산실적 / POS매출): [SALES_DASHBOARD_RESOURCE] READ — 화면 게이팅과 동일.
 * - 월별 투입적합성 / 배치 적합성: `monthly_sales_history` READ 유지 — 두 화면은 이번 자원 분리 대상이
 *   아니라 메뉴 게이팅 entity 가 그대로다. 셀렉터만 옮기면 메뉴는 보이는데 지점 목록만 403 이 된다.
 *
 * 여사원 일정관리의 `/team-schedule/branches` (`team_member_schedule` 가드) 를 빌려쓰면
 * `team_member_schedule` READ 없는 사용자가 지점 셀렉터에서 403 이 나므로 매출/실적 계열은
 * 화면 도메인 권한으로 가드하는 이 컨트롤러로 분리한다.
 *
 * 지점 목록 산출 — 배치 적합성을 뺀 4화면이 대시보드·근무형태별 여사원인원현황과 동일한 고정
 * 화이트리스트(34개, [DashboardBranchResolver]) 를 쓴다. 조직 트리를 직접 쓰던
 * (`WomenScheduleBranchResolver`) 전산실적/POS/투입적합성은 전사 권한자에게 `FS마케팅1팀` 같은
 * 팀 단위 조직까지 섞여 노출돼(Level5 부재 시 Level4 fallback) 인원현황 목록과 어긋났다.
 * 배치 적합성만 지점 단위 화면이라 행사마스터와 동일한 고정 지점 목록([WhitelistBranchScopeResolver]).
 *
 * 셀렉터 목록만 좁히며 실제 조회 스코프는 각 화면의 `@CurrentDataScope`(sharing policy) 기준 그대로다.
 */
@RestController
@RequestMapping("/api/v1/admin/sales")
class AdminSalesBranchController(
    private val dashboardBranchResolver: DashboardBranchResolver,
    private val whitelistBranchScopeResolver: WhitelistBranchScopeResolver,
) {

    /**
     * 월 매출(전산실적) 전용 지점 셀렉터 옵션 — 근무형태별 여사원인원현황과 동일 기준
     * ([DashboardBranchResolver], 전사 권한자 34개 화이트리스트 / 그 외 본인 조직 트리).
     */
    @GetMapping("/electronic/branches")
    @RequiresSfPermission(entity = SALES_DASHBOARD_RESOURCE, operation = SfPermissionOperation.READ)
    fun getElectronicSalesBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> =
        ResponseEntity.ok(ApiResponse.success(dashboardBranchResolver.resolveBranches(principal)))

    /**
     * POS매출 전용 지점 셀렉터 옵션 — 근무형태별 여사원인원현황과 동일 기준
     * ([DashboardBranchResolver], 전사 권한자 34개 화이트리스트 / 그 외 본인 조직 트리).
     */
    @GetMapping("/pos/branches")
    @RequiresSfPermission(entity = SALES_DASHBOARD_RESOURCE, operation = SfPermissionOperation.READ)
    fun getPosSalesBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> =
        ResponseEntity.ok(ApiResponse.success(dashboardBranchResolver.resolveBranches(principal)))

    /**
     * 월별 진열사원 투입적합성 전용 지점 셀렉터 옵션 — 근무형태별 여사원인원현황과 동일 기준
     * ([DashboardBranchResolver], 전사 권한자 34개 화이트리스트 / 그 외 본인 조직 트리).
     *
     * 두 화면은 같은 여사원 일정 축을 보는 화면이라 지점 셀렉터가 같아야 한다는 운영 요구.
     * 기존 `WomenScheduleBranchResolver` 직접 위임은 전사 권한자에게 `FS마케팅1팀` 같은 팀 단위 조직까지
     * 섞어 노출해(Level5 부재 시 Level4 fallback) 인원현황 화면의 34개 목록과 달랐다.
     *
     * 셀렉터 목록만 34개로 좁히며 실제 조회 스코프는 기존과 동일하다 — 조회는 `@CurrentDataScope`
     * (sharing policy) 기반이다.
     */
    @GetMapping("/input-adequacy/branches")
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    fun getInputAdequacyBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> =
        ResponseEntity.ok(ApiResponse.success(dashboardBranchResolver.resolveBranches(principal)))

    /**
     * 진열사원 배치 적합성 전용 지점 셀렉터 옵션 — 고정 지점 화이트리스트([WhitelistBranchScopeResolver]).
     *
     * 이 화면들이 예전에 쓰던 `WomenScheduleBranchResolver` 는 `Organization` 을 필터 없이 조회한 뒤
     * Level5(지점) 가 비어 있으면 Level4(팀) 로 fallback 하므로, 전사 권한자에게 `FS마케팅1팀` /
     * `FS판매전략팀` 같은 **팀 단위 조직까지 섞여 노출**된다. 배치 적합성은 지점 단위 화면이므로
     * 행사마스터·진열스케줄마스터와 동일한 고정 지점 목록(34개)을 쓴다.
     *
     * 셀렉터 목록만 바꾸며 실제 조회 스코프는 기존과 동일하다 — 조회는 `@CurrentDataScope`(sharing
     * policy) 기반이고 `AdminSalesComparisonService.applyScope` 가 교집합으로 재차 가드한다.
     */
    @GetMapping("/deployment/branches")
    @RequiresSfPermission(entity = "monthly_sales_history", operation = SfPermissionOperation.READ)
    fun getDeploymentBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> =
        ResponseEntity.ok(ApiResponse.success(whitelistBranchScopeResolver.getBranches(principal)))

    /**
     * 월 매출(물류배부) 전용 지점 셀렉터 옵션 — 대시보드 고정 화이트리스트(34개, [DashboardBranchResolver]).
     *
     * 셀렉터 목록만 34개로 좁히고 실제 조회/집계 스코프는 기존과 동일하다(셀렉터에서 고른 값은
     * 조직 트리 스코프의 부분집합).
     */
    @GetMapping("/monthly/branches")
    @RequiresSfPermission(entity = SALES_DASHBOARD_RESOURCE, operation = SfPermissionOperation.READ)
    fun getMonthlySalesBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> =
        ResponseEntity.ok(ApiResponse.success(dashboardBranchResolver.resolveBranches(principal)))
}
