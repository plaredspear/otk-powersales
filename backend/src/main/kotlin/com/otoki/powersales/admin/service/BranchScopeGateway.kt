package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.BranchScopeResult
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.admin.tools.branchscope.BranchScopeMode
import com.otoki.powersales.admin.tools.branchscope.service.BranchScopeModeStore
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import org.springframework.stereotype.Component

/**
 * 지점 선택 → 조회 경로의 단일 진입점 — 개발자 도구 토글([BranchScopeMode])로 신/구 방식을 전환한다.
 *
 * 지점 셀렉터가 있는 관리자 화면은 모두 이 게이트웨이를 경유한다. 화면군 차이는
 * [BranchScopeProfile] 로만 표현하고, 그 안에서
 * - `UNIFIED`(기본): [UnifiedBranchScopeResolver] — 셀렉터 목록 = 판정 화이트리스트, 판정 후 확장.
 * - `LEGACY`: 통합 이전 각 화면의 리졸버를 그대로 재현 ([WhitelistBranchScopeResolver] /
 *   [ReportBranchScopeService] / [DashboardBranchResolver] + [DataScope]).
 *
 * 전환의 영향을 운영에서 직접 비교하기 위한 **한시적** 컴포넌트다. 비교가 끝나면 호출부가
 * [UnifiedBranchScopeResolver] 를 직접 호출하도록 되돌리고 본 클래스와 [BranchScopeMode],
 * 그리고 LEGACY 재현 대상 리졸버들을 제거한다.
 */
@Component
class BranchScopeGateway(
    private val unifiedBranchScopeResolver: UnifiedBranchScopeResolver,
    private val dashboardBranchResolver: DashboardBranchResolver,
    private val whitelistBranchScopeResolver: WhitelistBranchScopeResolver,
    private val reportBranchScopeService: ReportBranchScopeService,
    private val womenScheduleBranchResolver: WomenScheduleBranchResolver,
    private val dataScopeService: AdminDataScopeService,
    private val branchCodeExpander: BranchCodeExpander,
    private val branchScopeModeStore: BranchScopeModeStore,
) {

    /** 현재 적용 중인 방식 — 응답에 실어 화면이 어느 방식의 수치인지 표시한다. */
    fun currentMode(): BranchScopeMode = branchScopeModeStore.getMode()

    /**
     * 지점 셀렉터 옵션.
     *
     * `UNIFIED` 는 화면군과 무관하게 "전사 34개(또는 조직 전건) / 비전사 조직 트리",
     * `LEGACY` 는 각 화면이 쓰던 리졸버 그대로다 (마스터 목록·보고서 계열은 비전사 본인 지점 1건).
     */
    fun resolveBranches(
        principal: WebUserPrincipal,
        profile: BranchScopeProfile,
    ): List<BranchResponse> = when (currentMode()) {
        BranchScopeMode.UNIFIED -> unifiedBranchScopeResolver.resolveBranches(principal, profile)
        BranchScopeMode.LEGACY -> when (profile) {
            BranchScopeProfile.DASHBOARD,
            BranchScopeProfile.SALES,
            -> dashboardBranchResolver.resolveBranches(principal)

            BranchScopeProfile.MASTER_LIST -> whitelistBranchScopeResolver.getBranches(principal)
            BranchScopeProfile.REPORT -> reportBranchScopeService.getBranches(principal)
            BranchScopeProfile.ORG_WIDE -> womenScheduleBranchResolver.resolveBranches(principal)
        }
    }

    /** 단일 선택 화면용 오버로드 — 대부분의 목록/보고서 화면이 지점을 하나만 고른다. */
    fun resolveScope(
        principal: WebUserPrincipal,
        requestedBranchCode: String?,
        profile: BranchScopeProfile,
    ): BranchScopeResult =
        resolveScope(principal, listOfNotNull(requestedBranchCode?.takeIf { it.isNotBlank() }), profile)

    /**
     * 조회 지점 스코프 산출.
     *
     * `UNIFIED` 는 [UnifiedBranchScopeResolver.resolveScope](셀렉터와 동일 출처 판정 + 확장),
     * `LEGACY` 는 화면군별 전환 이전 동작을 그대로 재현한다.
     */
    fun resolveScope(
        principal: WebUserPrincipal,
        requestedBranchCodes: List<String>?,
        profile: BranchScopeProfile,
    ): BranchScopeResult = when (currentMode()) {
        BranchScopeMode.UNIFIED -> unifiedBranchScopeResolver.resolveScope(principal, requestedBranchCodes, profile)
        BranchScopeMode.LEGACY -> legacyScope(principal, requestedBranchCodes, profile)
    }

    /**
     * 조회를 [DataScope] 로 가드하는 화면([BranchScopeProfile.SALES] / [BranchScopeProfile.ORG_WIDE]) 의
     * 지점 축 보정 — `UNIFIED` 는 셀렉터와 같은 조직 트리로 넓히고, `LEGACY` 는 손대지 않는다.
     *
     * @see UnifiedBranchScopeResolver.widenDataScope
     */
    fun applyDataScope(principal: WebUserPrincipal, scope: DataScope): DataScope = when (currentMode()) {
        BranchScopeMode.UNIFIED -> unifiedBranchScopeResolver.widenDataScope(principal, scope)
        BranchScopeMode.LEGACY -> scope
    }

    /**
     * 지점 선택이 **필수**인 화면(매출/실적 계열)의 조회 코드 산출 — [resolveScope] 결과를 서비스가
     * 그대로 쓰는 `List<String>` 로 변환한다.
     *
     * 차단([BranchScopeResult.NoAccess])은 빈 목록이 아니라 [NO_MATCH_CODES] 로 돌려준다 — 이 화면들의
     * 서비스는 "빈 목록 = 파라미터 누락(400)" 으로 검증하므로, 빈 목록을 넘기면 권한 차단이 400 으로
     * 뒤바뀐다. 매칭 불가능한 sentinel 코드를 넘겨 0건을 보장한다.
     */
    fun resolveQueryCodes(
        principal: WebUserPrincipal,
        requestedBranchCodes: List<String>,
        profile: BranchScopeProfile,
    ): List<String> = when (val result = resolveScope(principal, requestedBranchCodes, profile)) {
        is BranchScopeResult.Allowed -> result.queryCodes.ifEmpty { NO_MATCH_CODES }
        is BranchScopeResult.Unrestricted -> requestedBranchCodes
        is BranchScopeResult.NoAccess -> NO_MATCH_CODES
    }

    /**
     * 전환 이전 동작 재현.
     *
     * - [BranchScopeProfile.DASHBOARD]: [DashboardBranchResolver.effectiveBranchCodes](전사 34개 화이트리스트 /
     *   비전사 [DataScope.branchCodes] 교집합) 판정 + `AdminDashboardService` 가 수행하던 확장.
     * - [BranchScopeProfile.MASTER_LIST]: [WhitelistBranchScopeResolver](전사 34개 / 비전사 본인 1건, 확장 포함).
     * - [BranchScopeProfile.REPORT]: [ReportBranchScopeService](전사 미선택은 전건 / 비전사 본인 1건, 확장 포함).
     * - [BranchScopeProfile.SALES]: [DataScope] 교집합만 — 매출/실적 계열은 선택 코드를 확장하지 않았다.
     * - [BranchScopeProfile.ORG_WIDE]: 가시성은 [DataScope](별도 [applyDataScope] 경로) 가 판정하고,
     *   지점 필터는 선택값 확장뿐이었다 — 미선택이면 필터 없음.
     */
    private fun legacyScope(
        principal: WebUserPrincipal,
        requestedBranchCodes: List<String>?,
        profile: BranchScopeProfile,
    ): BranchScopeResult {
        val requested = requestedBranchCodes.orEmpty().filter { it.isNotBlank() }.distinct()
        return when (profile) {
            BranchScopeProfile.DASHBOARD -> {
                val scope = dataScopeService.resolve(principal)
                dashboardBranchResolver.effectiveBranchCodes(principal, scope, requested)
                    .toBranchScopeResult(requested, expand = true)
            }

            // resolver / service 가 이미 확장해 반환하므로 여기서 다시 확장하지 않는다 (2-hop 방지).
            BranchScopeProfile.MASTER_LIST ->
                whitelistBranchScopeResolver.effectiveBranchCodes(principal, requested.firstOrNull())
                    .toBranchScopeResult(requested, expand = false)

            BranchScopeProfile.REPORT ->
                reportBranchScopeService.expandedEffectiveBranchCodes(principal, requested.firstOrNull())
                    .toBranchScopeResult(requested, expand = false)

            BranchScopeProfile.SALES ->
                dataScopeService.resolve(principal).effectiveBranchCodes(requested)
                    .toBranchScopeResult(requested, expand = false)

            BranchScopeProfile.ORG_WIDE ->
                if (requested.isEmpty()) BranchScopeResult.Unrestricted
                else BranchScopeResult.Allowed(requested, branchCodeExpander.expand(requested).toList())
        }
    }

    /**
     * 기존 [EffectiveBranchResult] → [BranchScopeResult] 변환.
     *
     * `grantedCodes`(라벨 표기용 원본 코드) 는 선택값이 있으면 그 값, 없으면 판정 결과 코드를 쓴다 —
     * 확장까지 끝난 legacy 결과에서는 원본만 따로 복원할 수 없어 근사치이며, LEGACY 모드는 비교 검증
     * 용도라 라벨 근사로 충분하다.
     */
    private fun EffectiveBranchResult.toBranchScopeResult(
        requested: List<String>,
        expand: Boolean,
    ): BranchScopeResult = when (this) {
        is EffectiveBranchResult.All -> BranchScopeResult.Unrestricted
        is EffectiveBranchResult.Filtered -> BranchScopeResult.Allowed(
            grantedCodes = requested.ifEmpty { codes },
            queryCodes = if (expand) branchCodeExpander.expand(codes).toList() else codes,
        )

        is EffectiveBranchResult.NoAccess -> BranchScopeResult.NoAccess
    }

    companion object {
        /** 어떤 지점 코드와도 매칭되지 않는 sentinel — 권한 차단을 "0건" 으로 표현한다. */
        private val NO_MATCH_CODES = listOf("")
    }
}
