package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.BranchScopeResult
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.admin.dto.SelectorBranchResult
import com.otoki.powersales.admin.tools.branchscope.BranchScopeMode
import com.otoki.powersales.admin.tools.branchscope.service.BranchScopeModeStore
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * 개발자 도구 토글에 따른 신/구 지점 스코프 동작 대비 — 비교 대상이 되는 차이를 화면군별로 고정한다.
 *
 * 시나리오는 **상위 조직 사용자**(costCenterCode 가 영업부 코드 `5829`, 조직 트리에는 하위 지점 3건) 기준.
 * 이 계정이 셀렉터에 보이는 하위 지점을 골랐을 때 조회되는지가 통합의 핵심 차이다.
 */
@DisplayName("BranchScopeGateway 테스트 (지점 스코프 방식 토글)")
class BranchScopeGatewayTest {

    private val womenScheduleBranchResolver: WomenScheduleBranchResolver = mockk()
    private val dashboardBranchResolver = DashboardBranchResolver(womenScheduleBranchResolver)
    private val branchCodeExpander: BranchCodeExpander = mockk()
    private val unifiedBranchScopeResolver =
        UnifiedBranchScopeResolver(womenScheduleBranchResolver, dashboardBranchResolver, branchCodeExpander)
    private val whitelistBranchScopeResolver: WhitelistBranchScopeResolver = mockk()
    private val reportBranchScopeService: ReportBranchScopeService = mockk()
    private val dataScopeService: AdminDataScopeService = mockk()
    private val branchScopeModeStore: BranchScopeModeStore = mockk()

    private val gateway = BranchScopeGateway(
        unifiedBranchScopeResolver,
        dashboardBranchResolver,
        whitelistBranchScopeResolver,
        reportBranchScopeService,
        womenScheduleBranchResolver,
        dataScopeService,
        branchCodeExpander,
        branchScopeModeStore,
    )

    /** 상위 조직(영업부 코드 5829) 사용자 — 조직 트리에는 하위 지점 3건이 뜬다. */
    private val principal = principalOf(costCenterCode = "5829")

    /** 전사 권한자 — 셀렉터는 두 방식 모두 34개 화이트리스트. */
    private val admin = principalOf(costCenterCode = "9999", profileName = "시스템 관리자")

    private val treeBranches = listOf(
        BranchResponse("5826", "인천1지점"),
        BranchResponse("5827", "인천2지점"),
        BranchResponse("5828", "인천3지점"),
    )

    @BeforeEach
    fun setUp() {
        every { womenScheduleBranchResolver.resolveBranches(principal) } returns treeBranches
        every { womenScheduleBranchResolver.resolveBranches(admin) } returns
            DashboardBranchResolver.DASHBOARD_ALL_BRANCHES
        every { branchCodeExpander.expand(any()) } answers { firstArg<Collection<String>>().toSet() }
        // 레거시 경로의 판정 축 — 비전사 사용자의 DataScope 는 본인 코드 1건.
        every { dataScopeService.resolve(principal) } returns
            DataScope(branchCodes = listOf("5829"), isAllBranches = false)
        every { dataScopeService.resolve(admin) } returns
            DataScope(branchCodes = emptyList(), isAllBranches = true)
        // 레거시 마스터 목록/보고서 계열은 비전사에게 본인 지점 1건만 준다.
        every { whitelistBranchScopeResolver.getBranches(principal) } returns
            listOf(BranchResponse("5829", "3영업부"))
        every { reportBranchScopeService.getBranches(principal) } returns
            listOf(BranchResponse("5829", "3영업부"))
    }

    private fun mode(mode: BranchScopeMode) {
        every { branchScopeModeStore.getMode() } returns mode
    }

    @Nested
    @DisplayName("대시보드 (다중 선택)")
    inner class Dashboard {

        @Test
        @DisplayName("UNIFIED - 셀렉터에 보이는 하위 지점 선택이 조회된다")
        fun unifiedAllowsSelectorVisibleBranch() {
            mode(BranchScopeMode.UNIFIED)

            val result = gateway.resolveScope(principal, listOf("5827"), BranchScopeProfile.DASHBOARD)
                as BranchScopeResult.Allowed

            assertThat(result.grantedCodes).containsExactly("5827")
        }

        @Test
        @DisplayName("LEGACY - 같은 선택이 DataScope(본인 코드) 판정에 걸려 차단된다 (전환 전 동작 재현)")
        fun legacyBlocksSelectorVisibleBranch() {
            mode(BranchScopeMode.LEGACY)

            assertThat(gateway.resolveScope(principal, listOf("5827"), BranchScopeProfile.DASHBOARD))
                .isEqualTo(BranchScopeResult.NoAccess)
        }

        @Test
        @DisplayName("미선택 - UNIFIED 는 조직 트리 전체, LEGACY 는 본인 코드")
        fun noSelectionDiffersByMode() {
            mode(BranchScopeMode.UNIFIED)
            val unified = gateway.resolveScope(principal, null as List<String>?, BranchScopeProfile.DASHBOARD)
                as BranchScopeResult.Allowed
            assertThat(unified.grantedCodes).containsExactly("5826", "5827", "5828")

            mode(BranchScopeMode.LEGACY)
            val legacy = gateway.resolveScope(principal, null as List<String>?, BranchScopeProfile.DASHBOARD)
                as BranchScopeResult.Allowed
            assertThat(legacy.grantedCodes).containsExactly("5829")
        }

        @Test
        @DisplayName("LEGACY - 판정 통과 코드는 확장해 queryCodes 로 반환 (기존 service 확장 위치 이관분)")
        fun legacyExpandsGrantedCodes() {
            mode(BranchScopeMode.LEGACY)
            every { branchCodeExpander.expand(listOf("5829")) } returns setOf("5829", "5826", "5827", "5828")

            val result = gateway.resolveScope(principal, null as List<String>?, BranchScopeProfile.DASHBOARD)
                as BranchScopeResult.Allowed

            assertThat(result.grantedCodes).containsExactly("5829")
            assertThat(result.queryCodes).containsExactlyInAnyOrder("5829", "5826", "5827", "5828")
        }

        @Test
        @DisplayName("전사 권한자 - 두 모드 모두 34개 화이트리스트 (차이 없음)")
        fun allBranchesUserIdenticalAcrossModes() {
            mode(BranchScopeMode.UNIFIED)
            val unified = gateway.resolveScope(admin, null as List<String>?, BranchScopeProfile.DASHBOARD)
                as BranchScopeResult.Allowed

            mode(BranchScopeMode.LEGACY)
            val legacy = gateway.resolveScope(admin, null as List<String>?, BranchScopeProfile.DASHBOARD)
                as BranchScopeResult.Allowed

            assertThat(unified.grantedCodes).isEqualTo(legacy.grantedCodes)
            assertThat(unified.grantedCodes).hasSize(34)
        }
    }

    @Nested
    @DisplayName("마스터 목록 계열 (진열스케줄마스터·행사마스터)")
    inner class MasterList {

        @Test
        @DisplayName("셀렉터 - UNIFIED 는 조직 트리 3건, LEGACY 는 본인 지점 1건")
        fun selectorDiffersByMode() {
            mode(BranchScopeMode.UNIFIED)
            assertThat(gateway.resolveBranches(principal, BranchScopeProfile.MASTER_LIST).map { it.branchCode })
                .containsExactly("5826", "5827", "5828")

            mode(BranchScopeMode.LEGACY)
            assertThat(gateway.resolveBranches(principal, BranchScopeProfile.MASTER_LIST).map { it.branchCode })
                .containsExactly("5829")
        }

        @Test
        @DisplayName("UNIFIED - 셀렉터의 하위 지점 선택이 조회된다")
        fun unifiedAllowsTreeBranch() {
            mode(BranchScopeMode.UNIFIED)

            val result = gateway.resolveScope(principal, "5827", BranchScopeProfile.MASTER_LIST)

            assertThat(result.queryCodesOrNull()).containsExactly("5827")
        }

        @Test
        @DisplayName("LEGACY - 같은 선택이 본인 지점 1건 판정에 걸려 차단된다")
        fun legacyBlocksTreeBranch() {
            mode(BranchScopeMode.LEGACY)
            every {
                whitelistBranchScopeResolver.effectiveBranchCodes(principal, "5827")
            } returns EffectiveBranchResult.NoAccess

            assertThat(gateway.resolveScope(principal, "5827", BranchScopeProfile.MASTER_LIST).queryCodesOrNull())
                .isEmpty()
        }
    }

    @Nested
    @DisplayName("보고서 계열 (클레임·여사원 배치점검 등)")
    inner class Report {

        @Test
        @DisplayName("UNIFIED - 셀렉터의 하위 지점 선택이 조회된다")
        fun unifiedAllowsTreeBranch() {
            mode(BranchScopeMode.UNIFIED)

            val result = gateway.resolveScope(principal, "5828", BranchScopeProfile.REPORT)

            assertThat(result.queryCodesOrNull()).containsExactly("5828")
        }

        @Test
        @DisplayName("LEGACY - 전사 권한자 미선택은 전건(All) — 전환 전 보고서 동작")
        fun legacyAllBranchesUnrestricted() {
            mode(BranchScopeMode.LEGACY)
            every {
                reportBranchScopeService.expandedEffectiveBranchCodes(admin, null)
            } returns EffectiveBranchResult.All

            assertThat(gateway.resolveScope(admin, null as String?, BranchScopeProfile.REPORT))
                .isEqualTo(BranchScopeResult.Unrestricted)
        }

        @Test
        @DisplayName("UNIFIED - 전사 권한자 미선택은 34개로 좁혀진다 (셀렉터와 동일 범위)")
        fun unifiedAllBranchesRestrictedToWhitelist() {
            mode(BranchScopeMode.UNIFIED)

            val result = gateway.resolveScope(admin, null as String?, BranchScopeProfile.REPORT)

            assertThat((result as BranchScopeResult.Allowed).grantedCodes).hasSize(34)
        }
    }

    @Nested
    @DisplayName("매출/실적 계열 · 조직 전건 계열")
    inner class DataScopeFamilies {

        @Test
        @DisplayName("SALES - UNIFIED 는 선택 코드를 확장해 조회, LEGACY 는 DataScope 교집합(확장 없음)")
        fun salesDiffersByMode() {
            every { branchCodeExpander.expand(listOf("5827")) } returns setOf("5827", "5452")

            mode(BranchScopeMode.UNIFIED)
            assertThat(gateway.resolveQueryCodes(principal, listOf("5827"), BranchScopeProfile.SALES))
                .containsExactlyInAnyOrder("5827", "5452")

            mode(BranchScopeMode.LEGACY)
            // DataScope(본인 코드 5829) 와 교집합이 비어 차단 → 매칭 0건 sentinel
            assertThat(gateway.resolveQueryCodes(principal, listOf("5827"), BranchScopeProfile.SALES))
                .containsExactly("")
        }

        @Test
        @DisplayName("ORG_WIDE - 전사 권한자 미선택은 두 모드 모두 전건 (거래처 조회 범위 유지)")
        fun orgWideKeepsAllBranchesUnrestricted() {
            mode(BranchScopeMode.UNIFIED)
            assertThat(gateway.resolveScope(admin, null as String?, BranchScopeProfile.ORG_WIDE))
                .isEqualTo(BranchScopeResult.Unrestricted)

            mode(BranchScopeMode.LEGACY)
            assertThat(gateway.resolveScope(admin, null as String?, BranchScopeProfile.ORG_WIDE))
                .isEqualTo(BranchScopeResult.Unrestricted)
        }

        @Test
        @DisplayName("DataScope 지점 축 - UNIFIED 는 조직 트리로 넓히고(합집합), LEGACY 는 그대로")
        fun dataScopeWidening() {
            val scope = DataScope(branchCodes = listOf("5829"), isAllBranches = false)

            mode(BranchScopeMode.UNIFIED)
            assertThat(gateway.applyDataScope(principal, scope).branchCodes)
                .containsExactlyInAnyOrder("5829", "5826", "5827", "5828")

            mode(BranchScopeMode.LEGACY)
            assertThat(gateway.applyDataScope(principal, scope).branchCodes).containsExactly("5829")
        }

        @Test
        @DisplayName("DataScope 지점 축 - 전사 권한자는 두 모드 모두 그대로 (좁히지 않는다)")
        fun dataScopeAllBranchesUntouched() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            mode(BranchScopeMode.UNIFIED)
            assertThat(gateway.applyDataScope(admin, scope)).isEqualTo(scope)
        }
    }

    /**
     * 거래처 → 지점 셀렉터 역산 — "지점 먼저 선택" 선행 강제를 뒤집는 경로.
     *
     * 확장([BranchCodeExpander]) 에는 별칭뿐 아니라 롤업이 섞여 있어 후보가 둘 이상 나올 수 있다.
     * 그 경우 자동 선택하지 않고 화면에 판단을 넘기는 fail-safe 를 고정한다.
     */
    @Nested
    @DisplayName("거래처 → 지점 셀렉터 역산")
    inner class SelectorBranchReverseLookup {

        @Test
        @DisplayName("정확 일치 - 셀렉터 옵션과 같은 코드면 그대로 확정")
        fun exactMatch() {
            mode(BranchScopeMode.UNIFIED)

            val result = gateway.resolveSelectorBranch(principal, BranchScopeProfile.DASHBOARD, "5827")

            assertThat(result).isEqualTo(SelectorBranchResult.Resolved("5827", "인천2지점"))
        }

        @Test
        @DisplayName("확장 포함 1후보 - 상위/별칭 코드로 적재된 거래처도 지점이 확정된다")
        fun singleCandidateViaExpansion() {
            mode(BranchScopeMode.UNIFIED)
            every { branchCodeExpander.expand(setOf("5827")) } returns setOf("5827", "E5827")

            val result = gateway.resolveSelectorBranch(principal, BranchScopeProfile.DASHBOARD, "E5827")

            assertThat(result).isEqualTo(SelectorBranchResult.Resolved("5827", "인천2지점"))
        }

        @Test
        @DisplayName("롤업으로 후보 2건 - 자동 선택하지 않고 Ambiguous (엉뚱한 지점 선택 방지)")
        fun ambiguousWhenRollupOverlaps() {
            mode(BranchScopeMode.UNIFIED)
            every { branchCodeExpander.expand(setOf("5826")) } returns setOf("5826", "5452")
            every { branchCodeExpander.expand(setOf("5827")) } returns setOf("5827", "5452")

            val result = gateway.resolveSelectorBranch(principal, BranchScopeProfile.DASHBOARD, "5452")

            assertThat(result).isEqualTo(SelectorBranchResult.Ambiguous)
        }

        @Test
        @DisplayName("셀렉터 어디에도 없는 코드 - OutOfScope (권한 범위 밖 거래처)")
        fun outOfScope() {
            mode(BranchScopeMode.UNIFIED)

            assertThat(gateway.resolveSelectorBranch(principal, BranchScopeProfile.DASHBOARD, "9999"))
                .isEqualTo(SelectorBranchResult.OutOfScope)
        }

        @Test
        @DisplayName("null / blank - 역산 대상이 아니므로 OutOfScope, 다건 조회 결과에도 미포함")
        fun blankCodesExcluded() {
            mode(BranchScopeMode.UNIFIED)

            assertThat(gateway.resolveSelectorBranch(principal, BranchScopeProfile.DASHBOARD, null))
                .isEqualTo(SelectorBranchResult.OutOfScope)
            assertThat(gateway.resolveSelectorBranches(principal, BranchScopeProfile.DASHBOARD, listOf(null, "")))
                .isEmpty()
        }

        @Test
        @DisplayName("다건 - 거래처 코드별 결과를 한 번에 반환 (셀렉터 조회 1회)")
        fun batchLookup() {
            mode(BranchScopeMode.UNIFIED)

            val result = gateway.resolveSelectorBranches(
                principal,
                BranchScopeProfile.DASHBOARD,
                listOf("5826", "5828", "9999", "5826"),
            )

            assertThat(result).containsOnlyKeys("5826", "5828", "9999")
            assertThat(result["5826"]).isEqualTo(SelectorBranchResult.Resolved("5826", "인천1지점"))
            assertThat(result["5828"]).isEqualTo(SelectorBranchResult.Resolved("5828", "인천3지점"))
            assertThat(result["9999"]).isEqualTo(SelectorBranchResult.OutOfScope)
        }
    }

    private fun principalOf(
        costCenterCode: String?,
        profileName: String = "9. Staff",
    ): WebUserPrincipal =
        WebUserPrincipal(
            userId = 1L,
            usernameValue = "20030001",
            employeeCode = "20030001",
            employeeId = 1L,
            role = null,
            costCenterCode = costCenterCode,
            profileName = profileName,
            isSalesSupport = false,
            passwordChangeRequired = false,
            permissions = emptySet(),
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true,
        )
}
