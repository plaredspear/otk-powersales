package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("WomenScheduleBranchResolver 테스트")
class WomenScheduleBranchResolverTest {

    private val organizationRepository: OrganizationRepository = mockk(relaxUnitFun = true)

    private val resolver = WomenScheduleBranchResolver(organizationRepository)

    @Test
    @DisplayName("SYSTEM_ADMIN - 전체 Organization 조회")
    fun resolveBranches_systemAdmin() {
        val branches = listOf(
            BranchResponse("5460", "강남유통지점"),
            BranchResponse("5457", "강북유통지점"),
        )
        every { organizationRepository.findAllTeamScheduleBranches() } returns branches

        val result = resolver.resolveBranches(principalOf(costCenterCode = "9999", profileName = "시스템 관리자"))

        assertThat(result).hasSize(2)
        assertThat(result[0].branchCode).isEqualTo("5460")
        verify(exactly = 0) { organizationRepository.findTeamScheduleBranches(any(), any()) }
    }

    @Test
    @DisplayName("ALL_BRANCHES Role (영업지원실) - 전사 분기 (CVS 미포함)")
    fun resolveBranches_allBranchesRole() {
        val branches = listOf(BranchResponse("5460", "강남유통지점"))
        every { organizationRepository.findTeamScheduleBranches(null, true) } returns branches

        val result = resolver.resolveBranches(principalOf(costCenterCode = "3475", isSalesSupport = true))

        assertThat(result).hasSize(1)
        verify(exactly = 0) { organizationRepository.findAllTeamScheduleBranches() }
        verify { organizationRepository.findTeamScheduleBranches(null, true) }
    }

    @Test
    @DisplayName("ALL_BRANCHES Profile (본부장) - 전사 분기")
    fun resolveBranches_allBranchesProfile() {
        val branches = listOf(BranchResponse("5460", "강남유통지점"))
        every { organizationRepository.findTeamScheduleBranches(null, true) } returns branches

        val result = resolver.resolveBranches(principalOf(costCenterCode = "3475", profileName = "1.본부장"))

        assertThat(result).hasSize(1)
        verify { organizationRepository.findTeamScheduleBranches(null, true) }
    }

    @Test
    @DisplayName("일반 영업담당 (조장) - 본인 costCenterCode 기준 분기")
    fun resolveBranches_scopedRole() {
        val branches = listOf(BranchResponse("5457", "강북유통지점"))
        every { organizationRepository.findTeamScheduleBranches("5457", false) } returns branches

        val result = resolver.resolveBranches(principalOf(costCenterCode = "5457"))

        assertThat(result).hasSize(1)
        assertThat(result[0].branchCode).isEqualTo("5457")
        verify { organizationRepository.findTeamScheduleBranches("5457", false) }
    }

    @Test
    @DisplayName("isBranchAllowed - 허용 목록에 있으면 true, 없으면 false")
    fun isBranchAllowed() {
        every { organizationRepository.findTeamScheduleBranches("5457", false) } returns
            listOf(BranchResponse("5457", "강북유통지점"))
        val principal = principalOf(costCenterCode = "5457")

        assertThat(resolver.isBranchAllowed(principal, "5457")).isTrue()
        assertThat(resolver.isBranchAllowed(principal, "9999")).isFalse()
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
