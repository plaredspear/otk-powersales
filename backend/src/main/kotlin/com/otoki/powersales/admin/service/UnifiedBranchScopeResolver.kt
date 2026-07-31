package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.BranchScopeResult
import com.otoki.powersales.admin.dto.DataScope
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
 *      단 [BranchScopeProfile.ORG_WIDE] 화면은 종전대로 조직 전건 목록.
 *    - 비전사: 본인 costCenterCode 의 **조직 트리** ([WomenScheduleBranchResolver] —
 *      지점 사용자는 본인 지점 1건, 상위 조직(영업부·팀 코드) 사용자는 하위 지점 여러 건).
 * 2. **판정** ([resolveScope]): 요청 코드 ⊆ 셀렉터 목록. 밖의 코드가 하나라도 섞이면 전체 차단
 *    (NoAccess, fail-closed) — 셀렉터와 판정이 같은 출처라 정상 UI 에서는 밖의 코드가 올 수 없고,
 *    온 경우는 조작된 요청이므로 부분 허용하지 않는다.
 * 3. **확장**: 판정을 통과한 원본 코드만 [BranchCodeExpander] 로 넓혀 조회 필터([BranchScopeResult.Allowed.queryCodes])
 *    로 반환한다. 화이트리스트 판정 자체는 확장 전 원본으로 끝낸다 ([BranchCodeExpander] KDoc —
 *    화이트리스트를 확장하면 롤업 행이 권한 범위를 넓힌다). 소비자가 확장을 따로 수행할 필요가 없어
 *    화면별 확장 누락(거래처 조회·매출진도율마스터에서 실제 발생했던 유형)이 구조적으로 차단된다.
 * 4. **화면군 차이**([BranchScopeProfile]): 전사 권한자 범위(34개 제한 / 전건) 와 전사 셀렉터 목록만
 *    화면군별로 유지한다. 비전사 축(조직 트리) 과 확장 규칙은 모든 화면이 공유한다.
 *
 * ## 기존 리졸버와의 관계
 * - [DashboardBranchResolver]: 셀렉터는 같은 규칙(34개/조직 트리)이었으나 판정이 `DataScope.branchCodes`
 *   (원본 코드 1건) 기준이라, 상위 조직 사용자가 셀렉터에 보이는 하위 지점을 고르면 NoAccess 가 되는
 *   셀렉터·판정 불일치가 있었다. 본 리졸버는 판정을 셀렉터와 같은 출처로 맞춰 이를 해소한다.
 *   (34개 상수와 전사 판정 [DashboardBranchResolver.isAllBranches] 는 그대로 재사용.)
 * - [WhitelistBranchScopeResolver] / [ReportBranchScopeService]: 비전사 셀렉터가 본인 지점 1건 고정
 *   (조직 트리 미적용) 이었다. [BranchScopeProfile.MASTER_LIST] / [BranchScopeProfile.REPORT] 로 통합돼
 *   비전사 상위 조직 사용자도 셀렉터의 하위 지점을 조회할 수 있다.
 *
 * 전환 비교용 토글([com.otoki.powersales.admin.tools.branchscope.BranchScopeMode]) 이 걸려 있어,
 * 호출부는 본 리졸버를 직접 쓰지 않고 [BranchScopeGateway] 를 경유한다.
 */
@Component
class UnifiedBranchScopeResolver(
    private val womenScheduleBranchResolver: WomenScheduleBranchResolver,
    private val dashboardBranchResolver: DashboardBranchResolver,
    private val branchCodeExpander: BranchCodeExpander,
) {

    /**
     * 화면 지점 셀렉터 옵션 = 판정 화이트리스트 (동일 출처).
     * 전사 권한자 34개 고정([BranchScopeProfile.ORG_WIDE] 은 조직 전건) / 비전사 본인 조직 트리.
     * 목록이 비면 권한 지점 없는 사용자.
     */
    fun resolveBranches(
        principal: WebUserPrincipal,
        profile: BranchScopeProfile = BranchScopeProfile.DASHBOARD,
    ): List<BranchResponse> {
        return if (profile.allBranchesSelectorWhitelisted && dashboardBranchResolver.isAllBranches(principal)) {
            DashboardBranchResolver.DASHBOARD_ALL_BRANCHES
        } else {
            // 전사 권한자면 resolver 의 전사 분기(조직 전건), 비전사면 본인 조직 트리.
            womenScheduleBranchResolver.resolveBranches(principal)
        }
    }

    /**
     * 조회 지점 스코프 산출 — 판정(요청 ⊆ [resolveBranches]) → 확장([BranchCodeExpander]) 순서.
     *
     * @param requestedBranchCodes 화면 선택 지점 코드 (빈 값/중복 정리 후 판정). null/빈 목록이면
     *   미선택 = 화이트리스트 전체로 조회한다 (전사 34개 / 비전사 조직 트리 전체 — 지점 사용자는 본인 1건).
     *   단 전사 권한자를 제한하지 않는 화면([BranchScopeProfile.restrictsAllBranches] = false) 의 전사
     *   권한자는 [BranchScopeResult.Unrestricted](전건) — 종전 범위를 좁히지 않는다.
     */
    fun resolveScope(
        principal: WebUserPrincipal,
        requestedBranchCodes: List<String>?,
        profile: BranchScopeProfile = BranchScopeProfile.DASHBOARD,
    ): BranchScopeResult {
        val requested = requestedBranchCodes.orEmpty().filter { it.isNotBlank() }.distinct()
        val isAllBranches = dashboardBranchResolver.isAllBranches(principal)
        if (isAllBranches && !profile.restrictsAllBranches && requested.isEmpty()) {
            return BranchScopeResult.Unrestricted
        }

        val whitelist = resolveBranches(principal, profile).map { it.branchCode }
        if (whitelist.isEmpty()) return BranchScopeResult.NoAccess

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

    /**
     * `DataScope` 의 지점 축(legacy `branchCodes`) 을 셀렉터와 같은 출처로 넓힌다 —
     * 조회를 `DataScope` 로 가드하는 화면([BranchScopeProfile.SALES] / [BranchScopeProfile.ORG_WIDE]) 용.
     *
     * 비전사 사용자의 `DataScope.branchCodes` 는 본인 costCenterCode 1건이라, 셀렉터가 보여준 조직 트리의
     * 하위 지점을 골라도 가시성 판정에서 0건이 된다. 여기서 조직 트리(+`BranchMapping` 확장) 를 얹어
     * 셀렉터와 판정을 같은 범위로 맞춘다.
     *
     * **기존 코드와 합집합**으로 넓히기만 한다 — 상위 조직 코드(예: `5829`) 자체로 적재된 행이 트리 목록
     * (하위 지점 `5826`·`5827`·`5828`) 에는 없어서 빠지는 일을 막기 위함이다. 따라서 이 변환으로
     * 보이던 행이 사라지지 않는다(범위는 항상 종전 ⊆ 신규).
     *
     * 전사 권한자(`isAllBranches = true`) 는 그대로 둔다 — 이 화면들은 종전부터 전건이고, 좁히면 회귀다.
     */
    fun widenDataScope(principal: WebUserPrincipal, scope: DataScope): DataScope {
        if (scope.isAllBranches) return scope
        val treeCodes = womenScheduleBranchResolver.resolveBranches(principal).map { it.branchCode }
        if (treeCodes.isEmpty()) return scope
        val widened = (scope.branchCodes + branchCodeExpander.expand(treeCodes)).distinct()
        return scope.copy(branchCodes = widened)
    }
}
