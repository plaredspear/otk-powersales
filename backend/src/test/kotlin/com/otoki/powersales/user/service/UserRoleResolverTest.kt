package com.otoki.powersales.user.service

import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.organization.repository.OrganizationRepository
import com.otoki.powersales.organization.repository.dto.OrganizationCacheDto
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("UserRoleResolver — 영업지원실 / 영업본부 매칭 검증")
class UserRoleResolverTest {

    private val organizationRepository: OrganizationRepository = mockk()

    private val resolver = UserRoleResolver(
        organizationRepository,
    )

    @Test
    @DisplayName("OrgNameLevel4 contains '영업지원' (영업지원1팀) → true")
    fun level4ContainsSalesSupport() {
        stubOrg(costCenterCode = "5100", orgNameLevel4 = "영업지원1팀", orgNameLevel3 = "기타본부")
        val employee = createEmployee(costCenterCode = "5100")

        assertThat(resolver.isSalesSupport(employee)).isTrue
    }

    @Test
    @DisplayName("OrgNameLevel4 contains '영업지원' (영업지원2팀) → true")
    fun level4ContainsSalesSupport2() {
        stubOrg(costCenterCode = "5101", orgNameLevel4 = "영업지원2팀", orgNameLevel3 = "기타본부")
        val employee = createEmployee(costCenterCode = "5101")

        assertThat(resolver.isSalesSupport(employee)).isTrue
    }

    @Test
    @DisplayName("OrgNameLevel3 == '영업본부' → true")
    fun level3IsSalesHq() {
        stubOrg(costCenterCode = "1100", orgNameLevel4 = "기타팀", orgNameLevel3 = "영업본부")
        val employee = createEmployee(costCenterCode = "1100")

        assertThat(resolver.isSalesSupport(employee)).isTrue
    }

    @Test
    @DisplayName("Level3/4 모두 매칭 안 됨 → false")
    fun noMatch() {
        stubOrg(costCenterCode = "9999", orgNameLevel4 = "Retail팀", orgNameLevel3 = "Retail사업부")
        val employee = createEmployee(costCenterCode = "9999")

        assertThat(resolver.isSalesSupport(employee)).isFalse
    }

    @Test
    @DisplayName("Organization lookup 실패 → false")
    fun orgMissing() {
        // cascade Level5→4→3 모두 miss → null. Resolver 는 false 반환.
        every { organizationRepository.findFirstByOrgCodeCascade("0000") } returns null
        val employee = createEmployee(costCenterCode = "0000")

        assertThat(resolver.isSalesSupport(employee)).isFalse
    }

    @Test
    @DisplayName("costCenterCode null → false (cascade lookup 즉시 null)")
    fun costCenterCodeNull() {
        val employee = createEmployee(costCenterCode = null)

        assertThat(resolver.isSalesSupport(employee)).isFalse
    }

    @Test
    @DisplayName("helper — SF UserRole.Name contains '영업지원' → true")
    fun helperSalesSupportContains() {
        assertThat(resolver.isSalesSupportFromUserRoleName("영업지원1팀")).isTrue
        assertThat(resolver.isSalesSupportFromUserRoleName("영업지원2팀")).isTrue
        assertThat(resolver.isSalesSupportFromUserRoleName("영업지원실")).isTrue
    }

    @Test
    @DisplayName("helper — SF UserRole.Name == '영업본부' → true")
    fun helperSalesHqExact() {
        assertThat(resolver.isSalesSupportFromUserRoleName("영업본부")).isTrue
    }

    @Test
    @DisplayName("helper — 매칭 없음 → false")
    fun helperNoMatch() {
        assertThat(resolver.isSalesSupportFromUserRoleName("Retail사업부_1영업부")).isFalse
        assertThat(resolver.isSalesSupportFromUserRoleName("마케팅")).isFalse
    }

    @Test
    @DisplayName("helper — null / empty → false")
    fun helperNullOrEmpty() {
        assertThat(resolver.isSalesSupportFromUserRoleName(null)).isFalse
        assertThat(resolver.isSalesSupportFromUserRoleName("")).isFalse
    }

    private fun stubOrg(costCenterCode: String, orgNameLevel4: String?, orgNameLevel3: String?) {
        // cascade 메커니즘 자체는 OrganizationRepositoryCustomImpl 의 책임 — resolver 테스트는
        // cascade 결과 DTO 만 stub 하고 resolver 의 정책 분기를 검증.
        every { organizationRepository.findFirstByOrgCodeCascade(costCenterCode) } returns
            OrganizationCacheDto(
                orgCodeLevel3 = null,
                orgNameLevel3 = orgNameLevel3,
                orgNameLevel4 = orgNameLevel4,
                costCenterLevel3 = null,
            )
    }

    private fun createEmployee(costCenterCode: String?): Employee = Employee(
        employeeCode = "100234",
        name = "테스트사원",
        costCenterCode = costCenterCode
    )
}
