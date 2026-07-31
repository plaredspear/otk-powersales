package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.BranchScopeResult
import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.admin.tools.branchscope.BranchScopeMode
import com.otoki.powersales.admin.tools.branchscope.service.BranchScopeModeStore
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import org.springframework.stereotype.Component

/**
 * 투입현황 대시보드의 지점 스코프 산출 — 개발자 도구 토글([BranchScopeMode])로 신/구 방식을 전환한다.
 *
 * 전환의 영향을 운영에서 직접 비교하기 위한 **한시적** 컴포넌트다. 비교가 끝나면 컨트롤러가
 * [UnifiedBranchScopeResolver] 를 직접 호출하도록 되돌리고 본 클래스와 [BranchScopeMode] 를 제거한다.
 *
 * 셀렉터([resolveBranches])는 두 모드가 동일하므로 분기하지 않는다 — 달라지는 것은 판정/확장뿐이다.
 */
@Component
class DashboardBranchScopeGateway(
    private val unifiedBranchScopeResolver: UnifiedBranchScopeResolver,
    private val dashboardBranchResolver: DashboardBranchResolver,
    private val dataScopeService: AdminDataScopeService,
    private val branchCodeExpander: BranchCodeExpander,
    private val branchScopeModeStore: BranchScopeModeStore,
) {

    /** 현재 적용 중인 방식 — 응답에 실어 화면이 어느 방식의 수치인지 표시한다. */
    fun currentMode(): BranchScopeMode = branchScopeModeStore.getMode()

    /**
     * 지점 셀렉터 옵션. 두 모드 공통 (전사 34개 고정 / 비전사 본인 조직 트리) —
     * [UnifiedBranchScopeResolver.resolveBranches] 와 `DashboardBranchResolver.resolveBranches` 는
     * 같은 산출이므로 통합 리졸버 하나만 호출한다.
     */
    fun resolveBranches(principal: WebUserPrincipal): List<BranchResponse> =
        unifiedBranchScopeResolver.resolveBranches(principal)

    fun resolveScope(
        principal: WebUserPrincipal,
        requestedBranchCodes: List<String>?,
    ): BranchScopeResult = when (currentMode()) {
        BranchScopeMode.UNIFIED -> unifiedBranchScopeResolver.resolveScope(principal, requestedBranchCodes)
        BranchScopeMode.LEGACY -> legacyScope(principal, requestedBranchCodes)
    }

    /**
     * 전환 이전 동작 재현 — `DashboardBranchResolver.effectiveBranchCodes`(전사 34개 화이트리스트 /
     * 비전사 `DataScope.branchCodes` 교집합) 판정 + `AdminDashboardService` 가 수행하던 확장.
     *
     * 이전 컨트롤러의 결과 매핑을 그대로 옮긴다:
     * - `All`(전건, 지점 위임 경로에서만 발생) → 빈 목록 = repository 전건 조회 + 확장 없음
     * - `Filtered` → 그 코드 + 확장
     * - `NoAccess` → [BranchScopeResult.NoAccess] (컨트롤러가 sentinel 로 0건 보장)
     */
    private fun legacyScope(
        principal: WebUserPrincipal,
        requestedBranchCodes: List<String>?,
    ): BranchScopeResult {
        val scope = dataScopeService.resolve(principal)
        return when (
            val result = dashboardBranchResolver.effectiveBranchCodes(principal, scope, requestedBranchCodes)
        ) {
            is EffectiveBranchResult.All -> BranchScopeResult.Allowed(emptyList(), emptyList())
            is EffectiveBranchResult.Filtered -> BranchScopeResult.Allowed(
                grantedCodes = result.codes,
                queryCodes = branchCodeExpander.expand(result.codes).toList(),
            )
            is EffectiveBranchResult.NoAccess -> BranchScopeResult.NoAccess
        }
    }
}
