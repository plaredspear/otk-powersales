package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.BranchScopeResult
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import org.springframework.stereotype.Component

/**
 * 지점 스코프 통합 리졸버 — "셀렉터 목록 = 판정 화이트리스트, 판정 후 확장" 을 한 곳에서 보장한다.
 *
 * ## 규칙 (2026-07-31 정리)
 * 1. **셀렉터** ([resolveBranches]):
 *    - 전사 권한자: [DashboardBranchResolver.DASHBOARD_ALL_BRANCHES] 34개 고정 (운영 요구).
 *    - 비전사: 본인 costCenterCode 의 **조직 트리** ([WomenScheduleBranchResolver] —
 *      지점 사용자는 본인 지점 1건, 상위 조직(영업부·팀 코드) 사용자는 하위 지점 여러 건).
 * 2. **판정** ([resolveScope]): 요청 코드 ⊆ 셀렉터 목록. 밖의 코드가 하나라도 섞이면 전체 차단
 *    (NoAccess, fail-closed) — 셀렉터와 판정이 같은 출처라 정상 UI 에서는 밖의 코드가 올 수 없고,
 *    온 경우는 조작된 요청이므로 부분 허용하지 않는다.
 * 3. **확장**: 판정을 통과한 원본 코드만 [BranchCodeExpander] 로 넓혀 조회 필터([BranchScopeResult.Allowed.queryCodes])
 *    로 반환한다. 화이트리스트 판정 자체는 확장 전 원본으로 끝낸다 ([BranchCodeExpander] KDoc —
 *    화이트리스트를 확장하면 롤업 행이 권한 범위를 넓힌다). 소비자가 확장을 따로 수행할 필요가 없어
 *    화면별 확장 누락(거래처 조회·매출진도율마스터에서 실제 발생했던 유형)이 구조적으로 차단된다.
 *
 * ## 기존 리졸버와의 관계
 * - [DashboardBranchResolver]: 셀렉터는 같은 규칙(34개/조직 트리)이었으나 판정이 `DataScope.branchCodes`
 *   (원본 코드 1건) 기준이라, 상위 조직 사용자가 셀렉터에 보이는 하위 지점을 고르면 NoAccess 가 되는
 *   셀렉터·판정 불일치가 있었다. 본 리졸버는 판정을 셀렉터와 같은 출처로 맞춰 이를 해소한다.
 *   (34개 상수와 전사 판정 [DashboardBranchResolver.isAllBranches] 는 그대로 재사용.)
 * - [WhitelistBranchScopeResolver]: 판정 후 확장 구조는 동일하나 비전사 셀렉터가 본인 지점 1건 고정
 *   (조직 트리 미적용 — [ReportBranchScopeService] KDoc 의 의도적 결정). 보고서 계열의 통합 여부는
 *   별도 논의 후 결정한다.
 *
 * 현재 적용: 투입현황 대시보드 ([com.otoki.powersales.admin.controller.AdminDashboardController]).
 */
@Component
class UnifiedBranchScopeResolver(
    private val womenScheduleBranchResolver: WomenScheduleBranchResolver,
    private val dashboardBranchResolver: DashboardBranchResolver,
    private val branchCodeExpander: BranchCodeExpander,
) {

    /**
     * 화면 지점 셀렉터 옵션 = 판정 화이트리스트 (동일 출처).
     * 전사 권한자 34개 고정 / 비전사 본인 조직 트리. 목록이 비면 권한 지점 없는 사용자.
     */
    fun resolveBranches(principal: WebUserPrincipal): List<BranchResponse> {
        return if (dashboardBranchResolver.isAllBranches(principal)) {
            DashboardBranchResolver.DASHBOARD_ALL_BRANCHES
        } else {
            womenScheduleBranchResolver.resolveBranches(principal)
        }
    }

    /**
     * 조회 지점 스코프 산출 — 판정(요청 ⊆ [resolveBranches]) → 확장([BranchCodeExpander]) 순서.
     *
     * @param requestedBranchCodes 화면 선택 지점 코드 (빈 값/중복 정리 후 판정). null/빈 목록이면
     *   미선택 = 화이트리스트 전체로 조회한다 (전사 34개 / 비전사 조직 트리 전체 — 지점 사용자는 본인 1건).
     */
    fun resolveScope(
        principal: WebUserPrincipal,
        requestedBranchCodes: List<String>?,
    ): BranchScopeResult {
        val whitelist = resolveBranches(principal).map { it.branchCode }
        if (whitelist.isEmpty()) return BranchScopeResult.NoAccess

        val requested = requestedBranchCodes.orEmpty().filter { it.isNotBlank() }.distinct()
        val granted = when {
            requested.isEmpty() -> whitelist
            requested.all { it in whitelist } -> requested
            // 하나라도 화이트리스트 밖이면 전체 차단 — 부분 허용하지 않는다 (클래스 KDoc 규칙 2).
            else -> return BranchScopeResult.NoAccess
        }

        return BranchScopeResult.Allowed(
            grantedCodes = granted,
            queryCodes = branchCodeExpander.expand(granted).toList(),
        )
    }
}
