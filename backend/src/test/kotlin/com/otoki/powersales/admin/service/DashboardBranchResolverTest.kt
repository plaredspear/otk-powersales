package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DashboardBranchResolver 테스트")
class DashboardBranchResolverTest {

    private val womenScheduleBranchResolver: WomenScheduleBranchResolver = mockk()

    private val resolver = DashboardBranchResolver(womenScheduleBranchResolver)

    @Test
    @DisplayName("전사 권한자(SYSTEM_ADMIN) - 대시보드 고정 화이트리스트 34개 (Retail 32 + 영업지원2팀 + CVS전략팀)")
    fun resolveBranches_systemAdmin_fixedWhitelist() {
        val result = resolver.resolveBranches(principalOf(costCenterCode = "9999", profileName = "시스템 관리자"))

        assertThat(result).hasSize(34)
        assertThat(result).isEqualTo(DashboardBranchResolver.DASHBOARD_ALL_BRANCHES)
        // 공유 resolver 는 호출하지 않는다 (대시보드 전용 고정 목록).
        verify(exactly = 0) { womenScheduleBranchResolver.resolveBranches(any()) }
    }

    @Test
    @DisplayName("전사 권한자(영업지원) - 대시보드 고정 화이트리스트 34개")
    fun resolveBranches_salesSupport_fixedWhitelist() {
        val result = resolver.resolveBranches(principalOf(costCenterCode = "3475", isSalesSupport = true))

        assertThat(result).hasSize(34)
        assertThat(result).isEqualTo(DashboardBranchResolver.DASHBOARD_ALL_BRANCHES)
        verify(exactly = 0) { womenScheduleBranchResolver.resolveBranches(any()) }
    }

    @Test
    @DisplayName("전사 권한자(본부장) - 대시보드 고정 화이트리스트 34개")
    fun resolveBranches_hqManager_fixedWhitelist() {
        val result = resolver.resolveBranches(principalOf(costCenterCode = "3475", profileName = "1.본부장"))

        assertThat(result).hasSize(34)
        assertThat(result).isEqualTo(DashboardBranchResolver.DASHBOARD_ALL_BRANCHES)
    }

    @Test
    @DisplayName("본인 지점 스코프 사용자 - 공유 WomenScheduleBranchResolver 위임")
    fun resolveBranches_scopedUser_delegates() {
        val principal = principalOf(costCenterCode = "5457")
        val delegated = listOf(BranchResponse("5457", "강북유통지점"))
        every { womenScheduleBranchResolver.resolveBranches(principal) } returns delegated

        val result = resolver.resolveBranches(principal)

        assertThat(result).isEqualTo(delegated)
        verify { womenScheduleBranchResolver.resolveBranches(principal) }
    }

    @Test
    @DisplayName("고정 화이트리스트 - 영업지원2팀(4889)/CVS전략팀(5694) 포함 + org_cd 오름차순 정렬")
    fun dashboardAllBranches_contents() {
        val list = DashboardBranchResolver.DASHBOARD_ALL_BRANCHES

        // 영업지원2팀 + CVS전략팀 포함
        assertThat(list).contains(BranchResponse("4889", "영업지원2팀"))
        assertThat(list).contains(BranchResponse("5694", "CVS전략팀"))
        // Retail 32개 지점은 모두 이름이 '지점' 으로 끝남
        val retailBranches = list.filter { it.branchCode != "4889" && it.branchCode != "5694" }
        assertThat(retailBranches).hasSize(32)
        assertThat(retailBranches).allMatch { it.branchName.endsWith("지점") }
        // 코드 중복 없음
        assertThat(list.map { it.branchCode }).doesNotHaveDuplicates()
        // org_cd 오름차순 정렬 (4889 → 5694 → 5815 ...)
        assertThat(list.map { it.branchCode }).isSorted()
    }

    @Test
    @DisplayName("effectiveBranchCodes 전사 권한자 - 선택 없음 → 34개 화이트리스트 전체(Filtered)")
    fun effectiveBranchCodes_allBranches_noSelection() {
        val principal = principalOf(costCenterCode = "9999", profileName = "시스템 관리자")
        val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

        val result = resolver.effectiveBranchCodes(principal, scope, null)

        assertThat(result).isInstanceOf(EffectiveBranchResult.Filtered::class.java)
        val codes = (result as EffectiveBranchResult.Filtered).codes
        assertThat(codes).hasSize(34)
        assertThat(codes.toSet()).isEqualTo(DashboardBranchResolver.WHITELIST_CODES)
        verify(exactly = 0) { womenScheduleBranchResolver.resolveBranches(any()) }
    }

    @Test
    @DisplayName("effectiveBranchCodes 전사 권한자 - 34개 안 지점 선택 → 그 지점만(Filtered)")
    fun effectiveBranchCodes_allBranches_selectionInWhitelist() {
        val principal = principalOf(costCenterCode = "9999", profileName = "시스템 관리자")
        val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

        val result = resolver.effectiveBranchCodes(principal, scope, "5815")

        assertThat(result).isEqualTo(EffectiveBranchResult.Filtered(listOf("5815")))
    }

    @Test
    @DisplayName("effectiveBranchCodes 전사 권한자 - 34개 밖 지점 선택 → NoAccess(차단)")
    fun effectiveBranchCodes_allBranches_selectionOutsideWhitelist() {
        val principal = principalOf(costCenterCode = "9999", profileName = "시스템 관리자")
        val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

        val result = resolver.effectiveBranchCodes(principal, scope, "9999")

        assertThat(result).isEqualTo(EffectiveBranchResult.NoAccess)
    }

    @Test
    @DisplayName("effectiveBranchCodes 지점 사용자 - DataScope 위임(본인 지점)")
    fun effectiveBranchCodes_scopedUser_delegatesToDataScope() {
        val principal = principalOf(costCenterCode = "5457")
        val scope = DataScope(branchCodes = listOf("5457"), isAllBranches = false)

        val result = resolver.effectiveBranchCodes(principal, scope, null)

        assertThat(result).isEqualTo(EffectiveBranchResult.Filtered(listOf("5457")))
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
