package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ReportBranchScopeService 테스트")
class ReportBranchScopeServiceTest {

    private val dataScopeService: AdminDataScopeService = mockk()
    private val organizationRepository: OrganizationRepository = mockk()

    // BranchMapping 캐시가 비어 있으면 expand 는 입력을 그대로 돌려준다(pass-through).
    private val branchCodeExpander = BranchCodeExpander(mockk())

    private val service = ReportBranchScopeService(dataScopeService, organizationRepository, branchCodeExpander)

    /**
     * 전사 권한자 셀렉터 목록은 근무형태별 여사원인원현황·대시보드와 동일한 고정 34개다.
     * 종전 `Organization` 전건 조회는 Level5 부재 시 Level4(팀) 로 fallback 해 팀 단위 조직이 섞였다.
     */
    @Test
    @DisplayName("getBranches 전사 권한자 - 대시보드 고정 화이트리스트 34개 (Organization 미조회)")
    fun getBranches_allBranches_fixedWhitelist() {
        val principal = principalOf(costCenterCode = "9999")
        every { dataScopeService.resolve(principal) } returns
            DataScope(branchCodes = emptyList(), isAllBranches = true)

        val result = service.getBranches(principal)

        assertThat(result).isEqualTo(DashboardBranchResolver.DASHBOARD_ALL_BRANCHES)
        assertThat(result).hasSize(34)
        // 팀 단위 조직이 섞이던 조직 트리 전건 조회 경로를 더 이상 타지 않는다.
        assertThat(result.map { it.branchName }).allSatisfy { assertThat(it).doesNotContain("마케팅") }
    }

    /** 지점 사용자 목록은 종전대로 본인 costCenterCode 단일 지점 1건. */
    @Test
    @DisplayName("getBranches 지점 사용자 - 본인 지점 1건")
    fun getBranches_scopedUser_ownBranchOnly() {
        val principal = principalOf(costCenterCode = "5817")
        every { dataScopeService.resolve(principal) } returns
            DataScope(branchCodes = listOf("5817"), isAllBranches = false)
        every { organizationRepository.findFirstByAnyOrgCodeLevel("5817") } returns null

        assertThat(service.getBranches(principal)).isEmpty()
    }

    /**
     * 조회 필터용 산출은 판정(=[EffectiveBranchResult]) 을 확장 전 원본 코드로 끝낸 뒤 그 결과만 넓힌다.
     * BranchMapping 캐시가 비어 있는 본 테스트에서는 pass-through 라 코드가 그대로 유지된다.
     */
    @Test
    @DisplayName("expandedEffectiveBranchCodes - Filtered 결과를 BranchMapping 으로 확장")
    fun expandedEffectiveBranchCodes_expandsFiltered() {
        val principal = principalOf(costCenterCode = "5817")
        every { dataScopeService.resolve(principal) } returns
            DataScope(branchCodes = listOf("5817"), isAllBranches = false)

        val result = service.expandedEffectiveBranchCodes(principal, "5817")

        assertThat(result).isInstanceOf(EffectiveBranchResult.Filtered::class.java)
        assertThat((result as EffectiveBranchResult.Filtered).codes).containsExactly("5817")
    }

    /** 전사 권한자 + 지점 미선택은 종전대로 전건(All) — 확장 대상이 아니다. */
    @Test
    @DisplayName("expandedEffectiveBranchCodes - All 은 그대로 전달")
    fun expandedEffectiveBranchCodes_passesThroughAll() {
        val principal = principalOf(costCenterCode = "9999")
        every { dataScopeService.resolve(principal) } returns
            DataScope(branchCodes = emptyList(), isAllBranches = true)

        assertThat(service.expandedEffectiveBranchCodes(principal, null)).isEqualTo(EffectiveBranchResult.All)
    }

    private fun principalOf(costCenterCode: String?): WebUserPrincipal =
        WebUserPrincipal(
            userId = 1L,
            usernameValue = "20030001",
            employeeCode = "20030001",
            employeeId = 1L,
            role = null,
            costCenterCode = costCenterCode,
            profileName = "9. Staff",
            isSalesSupport = false,
            passwordChangeRequired = false,
            permissions = emptySet(),
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true,
        )
}
