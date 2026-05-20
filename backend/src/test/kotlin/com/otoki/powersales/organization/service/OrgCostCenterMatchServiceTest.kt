package com.otoki.powersales.organization.service

import com.otoki.powersales.organization.entity.Organization
import com.otoki.powersales.organization.repository.OrganizationRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("OrgCostCenterMatchService 테스트")
class OrgCostCenterMatchServiceTest {

    private val organizationRepository: OrganizationRepository = mockk()

    private val orgCostCenterMatchService = OrgCostCenterMatchService(
        organizationRepository,
    )

    @Test
    @DisplayName("OrgCodeLevel5 매칭 → CostCenterLevel5 반환")
    fun matchLevel5() {
        val org = createOrg()
        every { organizationRepository.findFirstByAnyOrgCodeLevel("3987") } returns org

        val result = orgCostCenterMatchService.findMatchingCostCenterCode("3987")

        assertThat(result).isPresent
        assertThat(result.get()).isEqualTo("GIC03")
    }

    @Test
    @DisplayName("OrgCodeLevel4 매칭 → CostCenterLevel4 반환")
    fun matchLevel4() {
        val org = createOrg()
        every { organizationRepository.findFirstByAnyOrgCodeLevel("744") } returns org

        val result = orgCostCenterMatchService.findMatchingCostCenterCode("744")

        assertThat(result).isPresent
        assertThat(result.get()).isEqualTo("GID91")
    }

    @Test
    @DisplayName("OrgCodeLevel3 매칭 → CostCenterLevel3 반환")
    fun matchLevel3() {
        val org = createOrg()
        every { organizationRepository.findFirstByAnyOrgCodeLevel("282") } returns org

        val result = orgCostCenterMatchService.findMatchingCostCenterCode("282")

        assertThat(result).isPresent
        assertThat(result.get()).isEqualTo("GIZ91")
    }

    @Test
    @DisplayName("OrgCodeLevel2 매칭 → CostCenterLevel2 반환")
    fun matchLevel2() {
        val org = createOrg()
        every { organizationRepository.findFirstByAnyOrgCodeLevel("279") } returns org

        val result = orgCostCenterMatchService.findMatchingCostCenterCode("279")

        assertThat(result).isPresent
        assertThat(result.get()).isEqualTo("GZZ91")
    }

    @Test
    @DisplayName("조회 결과 없음 → Optional.empty 반환")
    fun noOrgFound() {
        every { organizationRepository.findFirstByAnyOrgCodeLevel("NONEXISTENT") } returns null

        val result = orgCostCenterMatchService.findMatchingCostCenterCode("NONEXISTENT")

        assertThat(result).isEmpty
    }

    @Test
    @DisplayName("매칭된 CostCenter 코드가 null → Optional.empty 반환")
    fun matchedCostCenterIsNull() {
        // OrgCodeLevel5 만 매칭되지만 CostCenterLevel5 값이 null
        val org = Organization(
            id = 1L,
            costCenterLevel5 = null,
            orgCodeLevel5 = "9999"
        )
        every { organizationRepository.findFirstByAnyOrgCodeLevel("9999") } returns org

        val result = orgCostCenterMatchService.findMatchingCostCenterCode("9999")

        assertThat(result).isEmpty
    }

    @Test
    @DisplayName("매칭 분기 모두 실패 (어느 OrgCode 와도 일치 안 함) → Optional.empty 반환")
    fun matchedNoBranch() {
        // 쿼리는 매칭됐다고 가정하지만 실제로 어느 OrgCodeLevel 와도 정확히 일치하지 않는 경우
        val org = createOrg()
        every { organizationRepository.findFirstByAnyOrgCodeLevel("UNKNOWN") } returns org

        val result = orgCostCenterMatchService.findMatchingCostCenterCode("UNKNOWN")

        assertThat(result).isEmpty
    }

    @Test
    @DisplayName("빈 문자열 입력 → Optional.empty 반환 (조회 생략)")
    fun blankInput() {
        val result = orgCostCenterMatchService.findMatchingCostCenterCode("")

        assertThat(result).isEmpty
    }

    private fun createOrg(): Organization = Organization(
        id = 1L,
        costCenterLevel2 = "GZZ91",
        orgCodeLevel2 = "279",
        costCenterLevel3 = "GIZ91",
        orgCodeLevel3 = "282",
        costCenterLevel4 = "GID91",
        orgCodeLevel4 = "744",
        costCenterLevel5 = "GIC03",
        orgCodeLevel5 = "3987"
    )
}
