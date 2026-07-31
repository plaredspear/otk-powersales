package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.BranchScopeResult
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("UnifiedBranchScopeResolver 테스트")
class UnifiedBranchScopeResolverTest {

    private val womenScheduleBranchResolver: WomenScheduleBranchResolver = mockk()
    private val dashboardBranchResolver = DashboardBranchResolver(womenScheduleBranchResolver)
    private val branchCodeExpander: BranchCodeExpander = mockk()

    private val resolver = UnifiedBranchScopeResolver(
        womenScheduleBranchResolver,
        dashboardBranchResolver,
        branchCodeExpander,
    )

    @BeforeEach
    fun setUp() {
        // 확장 기본은 항등(입력 그대로) — 확장 반영 자체를 검증하는 테스트만 계보를 별도 stub.
        every { branchCodeExpander.expand(any()) } answers { firstArg<Collection<String>>().toSet() }
    }

    @Nested
    @DisplayName("resolveBranches - 셀렉터 화이트리스트")
    inner class ResolveBranchesTests {

        @Test
        @DisplayName("전사 권한자 - 대시보드 고정 화이트리스트 34개")
        fun allBranchesUser() {
            val result = resolver.resolveBranches(principalOf(costCenterCode = "9999", profileName = "시스템 관리자"))

            assertThat(result).isEqualTo(DashboardBranchResolver.DASHBOARD_ALL_BRANCHES)
            verify(exactly = 0) { womenScheduleBranchResolver.resolveBranches(any()) }
        }

        @Test
        @DisplayName("비전사 - 본인 조직 트리 (상위 조직 사용자는 하위 지점 여러 건)")
        fun branchUserGetsOrgTree() {
            val principal = principalOf(costCenterCode = "5829")
            every { womenScheduleBranchResolver.resolveBranches(principal) } returns listOf(
                BranchResponse("5826", "인천1지점"),
                BranchResponse("5827", "인천2지점"),
                BranchResponse("5828", "인천3지점"),
            )

            val result = resolver.resolveBranches(principal)

            assertThat(result.map { it.branchCode }).containsExactly("5826", "5827", "5828")
        }
    }

    @Nested
    @DisplayName("resolveScope - 판정(요청 ⊆ 셀렉터) 후 확장")
    inner class ResolveScopeTests {

        /** 비전사 상위 조직 사용자 — 셀렉터에 하위 지점 3건이 뜨는 상황. */
        private val principal = principalOf(costCenterCode = "5829")

        @BeforeEach
        fun stubTree() {
            every { womenScheduleBranchResolver.resolveBranches(principal) } returns listOf(
                BranchResponse("5826", "인천1지점"),
                BranchResponse("5827", "인천2지점"),
                BranchResponse("5828", "인천3지점"),
            )
        }

        @Test
        @DisplayName("미선택 - 화이트리스트 전체가 granted, 확장 결과가 queryCodes")
        fun noSelectionGrantsWholeWhitelist() {
            val result = resolver.resolveScope(principal, null)

            val allowed = result as BranchScopeResult.Allowed
            assertThat(allowed.grantedCodes).containsExactly("5826", "5827", "5828")
            assertThat(allowed.queryCodes).containsExactlyInAnyOrder("5826", "5827", "5828")
        }

        @Test
        @DisplayName("셀렉터에 보이는 지점 선택 - 판정 통과 (기존 DashboardBranchResolver 의 '보이는데 NoAccess' 해소)")
        fun selectorVisibleBranchIsAllowed() {
            val result = resolver.resolveScope(principal, listOf("5827"))

            val allowed = result as BranchScopeResult.Allowed
            assertThat(allowed.grantedCodes).containsExactly("5827")
        }

        @Test
        @DisplayName("확장은 판정 통과 코드에만 적용 - 이력 코드가 queryCodes 에 합류, grantedCodes 는 원본 유지")
        fun expandsGrantedCodesOnly() {
            // 인천2지점(5827) 의 조직 개편 전 이력 코드 5460 가정
            every { branchCodeExpander.expand(listOf("5827")) } returns setOf("5827", "5460")

            val result = resolver.resolveScope(principal, listOf("5827"))

            val allowed = result as BranchScopeResult.Allowed
            assertThat(allowed.grantedCodes).containsExactly("5827")
            assertThat(allowed.queryCodes).containsExactlyInAnyOrder("5827", "5460")
        }

        @Test
        @DisplayName("화이트리스트 밖 코드가 하나라도 섞이면 전체 차단 (부분 허용 없음) + 확장 미호출")
        fun mixedSelectionFullyBlocked() {
            val result = resolver.resolveScope(principal, listOf("5827", "9999"))

            assertThat(result).isEqualTo(BranchScopeResult.NoAccess)
            verify(exactly = 0) { branchCodeExpander.expand(any()) }
        }

        @Test
        @DisplayName("빈 값/중복 코드는 정리 후 판정")
        fun normalizesBlankAndDuplicate() {
            val result = resolver.resolveScope(principal, listOf("5827", "", " ", "5827"))

            val allowed = result as BranchScopeResult.Allowed
            assertThat(allowed.grantedCodes).containsExactly("5827")
        }

        @Test
        @DisplayName("권한 지점 없는 사용자(화이트리스트 빈 목록) - NoAccess")
        fun emptyWhitelistBlocked() {
            val orphan = principalOf(costCenterCode = null)
            every { womenScheduleBranchResolver.resolveBranches(orphan) } returns emptyList()

            assertThat(resolver.resolveScope(orphan, null)).isEqualTo(BranchScopeResult.NoAccess)
        }

        @Test
        @DisplayName("전사 권한자 미선택 - 34개 화이트리스트 전체가 granted (전건 아님)")
        fun allBranchesUserNoSelection() {
            val admin = principalOf(costCenterCode = "9999", profileName = "시스템 관리자")

            val result = resolver.resolveScope(admin, null)

            val allowed = result as BranchScopeResult.Allowed
            assertThat(allowed.grantedCodes).hasSize(34)
            assertThat(allowed.grantedCodes).isEqualTo(DashboardBranchResolver.WHITELIST_CODES.toList())
        }

        @Test
        @DisplayName("전사 권한자 + 34개 밖 선택 - NoAccess")
        fun allBranchesUserOutsideWhitelistBlocked() {
            val admin = principalOf(costCenterCode = "9999", profileName = "시스템 관리자")

            assertThat(resolver.resolveScope(admin, listOf("0000"))).isEqualTo(BranchScopeResult.NoAccess)
        }
    }

    private fun principalOf(
        costCenterCode: String?,
        profileName: String = "9. Staff",
        isSalesSupport: Boolean = false,
    ): WebUserPrincipal =
        WebUserPrincipal(
            userId = 1L,
            usernameValue = "20030001",
            employeeCode = "20030001",
            employeeId = 1L,
            role = null,
            costCenterCode = costCenterCode,
            profileName = profileName,
            isSalesSupport = isSalesSupport,
            passwordChangeRequired = false,
            permissions = emptySet(),
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true,
        )
}
