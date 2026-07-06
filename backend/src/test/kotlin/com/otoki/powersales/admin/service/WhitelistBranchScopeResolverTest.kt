package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("WhitelistBranchScopeResolver 테스트")
class WhitelistBranchScopeResolverTest {

    private val reportBranchScopeService: ReportBranchScopeService = mockk()
    private val dashboardBranchResolver: DashboardBranchResolver = mockk()

    private val resolver = WhitelistBranchScopeResolver(reportBranchScopeService, dashboardBranchResolver)

    @Test
    @DisplayName("getBranches 전사 권한자 - 대시보드 고정 화이트리스트 34개")
    fun getBranches_allBranches_fixedWhitelist() {
        val principal = principalOf(costCenterCode = "9999", profileName = "시스템 관리자")
        every { dashboardBranchResolver.isAllBranches(principal) } returns true

        val result = resolver.getBranches(principal)

        assertThat(result).isEqualTo(DashboardBranchResolver.DASHBOARD_ALL_BRANCHES)
        assertThat(result).hasSize(34)
        verify(exactly = 0) { reportBranchScopeService.getBranches(any()) }
    }

    @Test
    @DisplayName("getBranches 지점 사용자 - ReportBranchScopeService 위임(본인 지점)")
    fun getBranches_scopedUser_delegates() {
        val principal = principalOf(costCenterCode = "5457")
        val delegated = listOf(BranchResponse("5457", "강북유통지점"))
        every { dashboardBranchResolver.isAllBranches(principal) } returns false
        every { reportBranchScopeService.getBranches(principal) } returns delegated

        val result = resolver.getBranches(principal)

        assertThat(result).isEqualTo(delegated)
        verify { reportBranchScopeService.getBranches(principal) }
    }

    @Test
    @DisplayName("effectiveBranchCodes 전사 권한자 - 선택 없음 → 34개 전체(Filtered)")
    fun effectiveBranchCodes_allBranches_noSelection() {
        val principal = principalOf(costCenterCode = "9999", profileName = "시스템 관리자")
        every { dashboardBranchResolver.isAllBranches(principal) } returns true

        val result = resolver.effectiveBranchCodes(principal, null)

        assertThat(result).isInstanceOf(EffectiveBranchResult.Filtered::class.java)
        val codes = (result as EffectiveBranchResult.Filtered).codes
        assertThat(codes).hasSize(34)
        assertThat(codes.toSet()).isEqualTo(DashboardBranchResolver.WHITELIST_CODES)
    }

    @Test
    @DisplayName("effectiveBranchCodes 전사 권한자 - 34개 안 지점 선택 → 그 지점(Filtered)")
    fun effectiveBranchCodes_allBranches_selectionInWhitelist() {
        val principal = principalOf(costCenterCode = "9999", profileName = "시스템 관리자")
        every { dashboardBranchResolver.isAllBranches(principal) } returns true

        val result = resolver.effectiveBranchCodes(principal, "5815")

        assertThat(result).isEqualTo(EffectiveBranchResult.Filtered(listOf("5815")))
    }

    @Test
    @DisplayName("effectiveBranchCodes 전사 권한자 - 34개 밖 지점 선택 → NoAccess(차단)")
    fun effectiveBranchCodes_allBranches_selectionOutsideWhitelist() {
        val principal = principalOf(costCenterCode = "9999", profileName = "시스템 관리자")
        every { dashboardBranchResolver.isAllBranches(principal) } returns true

        val result = resolver.effectiveBranchCodes(principal, "9999")

        assertThat(result).isEqualTo(EffectiveBranchResult.NoAccess)
    }

    @Test
    @DisplayName("effectiveBranchCodes 지점 사용자 - ReportBranchScopeService 위임")
    fun effectiveBranchCodes_scopedUser_delegates() {
        val principal = principalOf(costCenterCode = "5457")
        every { dashboardBranchResolver.isAllBranches(principal) } returns false
        every { reportBranchScopeService.effectiveBranchCodes(principal, null) } returns
            EffectiveBranchResult.Filtered(listOf("5457"))

        val result = resolver.effectiveBranchCodes(principal, null)

        assertThat(result).isEqualTo(EffectiveBranchResult.Filtered(listOf("5457")))
        verify { reportBranchScopeService.effectiveBranchCodes(principal, null) }
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
