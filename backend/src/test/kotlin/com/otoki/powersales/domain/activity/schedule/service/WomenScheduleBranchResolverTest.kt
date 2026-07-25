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
    @DisplayName("isAllBranchLookupUser - 영업지원2팀(4889) 이면 true")
    fun isAllBranchLookupUser_salesSupport2() {
        assertThat(resolver.isAllBranchLookupUser(principalOf(costCenterCode = "4889"))).isTrue()
    }

    @Test
    @DisplayName("isAllBranchLookupUser - 그 외 costCenterCode / null 이면 false")
    fun isAllBranchLookupUser_others() {
        assertThat(resolver.isAllBranchLookupUser(principalOf(costCenterCode = "5457"))).isFalse()
        assertThat(resolver.isAllBranchLookupUser(principalOf(costCenterCode = null))).isFalse()
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

    @Test
    @DisplayName("isAllBranchesUser - 전사 프로필(시스템 관리자/영업지원/본부장·사업부장·영업부장) 이면 true")
    fun isAllBranchesUser_allBranchProfiles() {
        assertThat(resolver.isAllBranchesUser(principalOf(costCenterCode = "9999", profileName = "시스템 관리자")))
            .isTrue()
        assertThat(resolver.isAllBranchesUser(principalOf(costCenterCode = "3475", isSalesSupport = true)))
            .isTrue()
        assertThat(resolver.isAllBranchesUser(principalOf(costCenterCode = "3475", profileName = "1.본부장")))
            .isTrue()
        assertThat(resolver.isAllBranchesUser(principalOf(costCenterCode = "3475", profileName = "3.영업부장")))
            .isTrue()
    }

    @Test
    @DisplayName("isAllBranchesUser - 지점 권한자(조장 등) 는 false. 4889 단독으로는 전사가 되지 않는다")
    fun isAllBranchesUser_scopedUsers() {
        assertThat(resolver.isAllBranchesUser(principalOf(costCenterCode = "5457"))).isFalse()
        assertThat(resolver.isAllBranchesUser(principalOf(costCenterCode = null))).isFalse()
        // 4889(영업지원2팀) 는 isAllBranchLookupUser 로만 전사 — resolveBranches 의 전사 분기에 없는
        // 조건이므로 isAllBranchesUser 에 포함하면 셀렉터(조직 트리)↔조회(전사) 가 fail-open 으로 갈라진다.
        assertThat(resolver.isAllBranchesUser(principalOf(costCenterCode = "4889"))).isFalse()
    }

    @Test
    @DisplayName("드리프트 가드 - isAllBranchesUser 와 resolveBranches 의 전사 분기 판정이 항상 일치")
    fun isAllBranchesUser_matchesResolveBranchesAllBranchBranch() {
        // resolveBranches 가 어떤 분기를 탔는지 반환값으로 식별하기 위해 분기별 마커 지점을 stub 한다.
        val allBranchMarker = listOf(BranchResponse("ALL", "전사"))
        val scopedMarker = listOf(BranchResponse("SCOPED", "본인지점"))
        every { organizationRepository.findAllTeamScheduleBranches() } returns allBranchMarker
        every { organizationRepository.findTeamScheduleBranches(null, true) } returns allBranchMarker
        every { organizationRepository.findTeamScheduleBranches(any(), false) } returns scopedMarker

        val principals = listOf(
            principalOf(costCenterCode = "9999", profileName = "시스템 관리자"),
            principalOf(costCenterCode = "3475", isSalesSupport = true),
            principalOf(costCenterCode = "3475", profileName = "1.본부장"),
            principalOf(costCenterCode = "3475", profileName = "2.사업부장"),
            principalOf(costCenterCode = "3475", profileName = "3.영업부장"),
            principalOf(costCenterCode = "5457"),
            // 영업지원2팀 — isSalesSupport 캐시 컬럼이 아직 false 인 상태를 재현 (fail-open 회귀 방지 핵심 케이스)
            principalOf(costCenterCode = "4889"),
            principalOf(costCenterCode = null),
        )

        for (principal in principals) {
            val tookAllBranchBranch = resolver.resolveBranches(principal) == allBranchMarker
            assertThat(resolver.isAllBranchesUser(principal))
                .describedAs("costCenterCode=%s profile=%s", principal.costCenterCode, principal.profileName)
                .isEqualTo(tookAllBranchBranch)
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
