package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.BranchScopeResult
import com.otoki.powersales.admin.dto.DataScope
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
import org.junit.jupiter.api.Test

/**
 * 개발자 도구 토글에 따른 신/구 지점 스코프 동작 대비 — 비교 대상이 되는 차이를 고정한다.
 * 시나리오는 **상위 조직 사용자**(costCenterCode 가 영업부 코드, 셀렉터에 하위 지점 3건) 기준.
 */
@DisplayName("DashboardBranchScopeGateway 테스트 (지점 스코프 방식 토글)")
class DashboardBranchScopeGatewayTest {

    private val womenScheduleBranchResolver: WomenScheduleBranchResolver = mockk()
    private val dashboardBranchResolver = DashboardBranchResolver(womenScheduleBranchResolver)
    private val unifiedBranchScopeResolver = UnifiedBranchScopeResolver(
        womenScheduleBranchResolver,
        dashboardBranchResolver,
        mockk<BranchCodeExpander>().also {
            every { it.expand(any()) } answers { firstArg<Collection<String>>().toSet() }
        },
    )
    private val dataScopeService: AdminDataScopeService = mockk()
    private val branchCodeExpander: BranchCodeExpander = mockk()
    private val branchScopeModeStore: BranchScopeModeStore = mockk()

    private val gateway = DashboardBranchScopeGateway(
        unifiedBranchScopeResolver,
        dashboardBranchResolver,
        dataScopeService,
        branchCodeExpander,
        branchScopeModeStore,
    )

    /** 상위 조직(영업부 코드 5829) 사용자 — 셀렉터에는 하위 지점 3건이 뜬다. */
    private val principal = principalOf(costCenterCode = "5829")

    @BeforeEach
    fun setUp() {
        every { womenScheduleBranchResolver.resolveBranches(principal) } returns listOf(
            BranchResponse("5826", "인천1지점"),
            BranchResponse("5827", "인천2지점"),
            BranchResponse("5828", "인천3지점"),
        )
        every { branchCodeExpander.expand(any()) } answers { firstArg<Collection<String>>().toSet() }
        // 레거시 경로의 판정 축 — 비전사 사용자의 DataScope 는 본인 코드 1건.
        every { dataScopeService.resolve(principal) } returns
            DataScope(branchCodes = listOf("5829"), isAllBranches = false)
    }

    @Test
    @DisplayName("셀렉터는 두 모드 공통 — 모드와 무관하게 조직 트리 목록")
    fun selectorIsSharedAcrossModes() {
        every { branchScopeModeStore.getMode() } returns BranchScopeMode.LEGACY

        assertThat(gateway.resolveBranches(principal).map { it.branchCode })
            .containsExactly("5826", "5827", "5828")
    }

    @Test
    @DisplayName("UNIFIED - 셀렉터에 보이는 하위 지점 선택이 조회된다")
    fun unifiedAllowsSelectorVisibleBranch() {
        every { branchScopeModeStore.getMode() } returns BranchScopeMode.UNIFIED

        val result = gateway.resolveScope(principal, listOf("5827")) as BranchScopeResult.Allowed

        assertThat(result.grantedCodes).containsExactly("5827")
    }

    @Test
    @DisplayName("LEGACY - 같은 선택이 DataScope(본인 코드) 판정에 걸려 차단된다 (전환 전 동작 재현)")
    fun legacyBlocksSelectorVisibleBranch() {
        every { branchScopeModeStore.getMode() } returns BranchScopeMode.LEGACY

        assertThat(gateway.resolveScope(principal, listOf("5827"))).isEqualTo(BranchScopeResult.NoAccess)
    }

    @Test
    @DisplayName("미선택 - UNIFIED 는 조직 트리 전체, LEGACY 는 본인 코드")
    fun noSelectionDiffersByMode() {
        every { branchScopeModeStore.getMode() } returns BranchScopeMode.UNIFIED
        val unified = gateway.resolveScope(principal, null) as BranchScopeResult.Allowed
        assertThat(unified.grantedCodes).containsExactly("5826", "5827", "5828")

        every { branchScopeModeStore.getMode() } returns BranchScopeMode.LEGACY
        val legacy = gateway.resolveScope(principal, null) as BranchScopeResult.Allowed
        assertThat(legacy.grantedCodes).containsExactly("5829")
    }

    @Test
    @DisplayName("LEGACY - 판정 통과 코드는 확장해 queryCodes 로 반환 (기존 service 확장 위치 이관분)")
    fun legacyExpandsGrantedCodes() {
        every { branchScopeModeStore.getMode() } returns BranchScopeMode.LEGACY
        every { branchCodeExpander.expand(listOf("5829")) } returns setOf("5829", "5826", "5827", "5828")

        val result = gateway.resolveScope(principal, null) as BranchScopeResult.Allowed

        assertThat(result.grantedCodes).containsExactly("5829")
        assertThat(result.queryCodes).containsExactlyInAnyOrder("5829", "5826", "5827", "5828")
    }

    @Test
    @DisplayName("전사 권한자 - 두 모드 모두 34개 화이트리스트 (차이 없음)")
    fun allBranchesUserIdenticalAcrossModes() {
        val admin = principalOf(costCenterCode = "9999", profileName = "시스템 관리자")
        every { dataScopeService.resolve(admin) } returns
            DataScope(branchCodes = emptyList(), isAllBranches = true)

        every { branchScopeModeStore.getMode() } returns BranchScopeMode.UNIFIED
        val unified = gateway.resolveScope(admin, null) as BranchScopeResult.Allowed

        every { branchScopeModeStore.getMode() } returns BranchScopeMode.LEGACY
        val legacy = gateway.resolveScope(admin, null) as BranchScopeResult.Allowed

        assertThat(unified.grantedCodes).isEqualTo(legacy.grantedCodes)
        assertThat(unified.grantedCodes).hasSize(34)
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
