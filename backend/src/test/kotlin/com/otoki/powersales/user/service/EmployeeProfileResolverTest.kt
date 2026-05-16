package com.otoki.powersales.user.service

import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.organization.entity.Organization
import com.otoki.powersales.organization.repository.OrganizationRepository
import com.otoki.powersales.user.entity.ProfileType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("EmployeeProfileResolver — SF AppointmentTriggerHandler 10개 분기 동등 검증")
class EmployeeProfileResolverTest {

    @Mock
    private lateinit var organizationRepository: OrganizationRepository

    @InjectMocks
    private lateinit var resolver: EmployeeProfileResolver

    @Test
    @DisplayName("[Branch 1] orgCodeLevel3=='5066' (마케팅실) → MARKETING — 직책 무관")
    fun marketing() {
        stubOrg(costCenterCode = "5066", orgCodeLevel3 = "5066")
        val employee = createEmployee(costCenterCode = "5066", jikchak = "사원")

        assertThat(resolver.resolve(employee)).isEqualTo(ProfileType.MARKETING)
    }

    @Test
    @DisplayName("[Branch 2] orgCodeLevel3=='3475' (지원실) + 판매/조장 아님 → STAFF")
    fun supportStaff() {
        stubOrg(costCenterCode = "3475", orgCodeLevel3 = "3475")
        val employee = createEmployee(costCenterCode = "3475", jikchak = "사원")

        assertThat(resolver.resolve(employee)).isEqualTo(ProfileType.STAFF)
    }

    @Test
    @DisplayName("[Branch 2 negative] orgCodeLevel3=='3475' + 판매팀장 → STAFF 매칭 안 됨, fall-through")
    fun supportSalesTeamLeader() {
        stubOrg(costCenterCode = "3475", orgCodeLevel3 = "3475")
        val employee = createEmployee(costCenterCode = "3475", jikchak = "판매팀장")

        // Branch 2 (STAFF) 매칭 안 됨 → Branch 5 (TEAM_LEADER) 매칭 ('판매팀장' == '판매팀장')
        assertThat(resolver.resolve(employee)).isEqualTo(ProfileType.TEAM_LEADER)
    }

    @Test
    @DisplayName("[Branch 3] orgCodeLevel3=='3472' (판매전략팀) → STAFF — 직책 무관")
    fun salesStrategyStaff() {
        stubOrg(costCenterCode = "3472", orgCodeLevel3 = "3472")
        val employee = createEmployee(costCenterCode = "3472", jikchak = "지점장")

        assertThat(resolver.resolve(employee)).isEqualTo(ProfileType.STAFF)
    }

    @Test
    @DisplayName("[Branch 4] costCenterCode in {5397, 5398, 5639} (BS팀/SP팀) → STAFF")
    fun bsSpTeamStaff() {
        stubOrg(costCenterCode = "5397", orgCodeLevel3 = "1100")
        val employee = createEmployee(costCenterCode = "5397", jikchak = "팀장")

        assertThat(resolver.resolve(employee)).isEqualTo(ProfileType.STAFF)
    }

    @Test
    @DisplayName("[Branch 5] jikchak contains '조장' → TEAM_LEADER")
    fun teamLeaderByJikchak() {
        stubOrg(costCenterCode = "1100", orgCodeLevel3 = "1100")
        val employee = createEmployee(costCenterCode = "1100", jikchak = "판매조장")

        assertThat(resolver.resolve(employee)).isEqualTo(ProfileType.TEAM_LEADER)
    }

    @Test
    @DisplayName("[Branch 6] jikchak contains '지점장' → BRANCH_MANAGER")
    fun branchManagerByJikchak() {
        stubOrg(costCenterCode = "1100", orgCodeLevel3 = "1100")
        val employee = createEmployee(costCenterCode = "1100", jikchak = "지점장직무대행")

        assertThat(resolver.resolve(employee)).isEqualTo(ProfileType.BRANCH_MANAGER)
    }

    @Test
    @DisplayName("[Branch 7] jikchak contains '부장' + '사업' → BUSINESS_DIRECTOR")
    fun businessDirector() {
        stubOrg(costCenterCode = "1100", orgCodeLevel3 = "1100")
        val employee = createEmployee(costCenterCode = "1100", jikchak = "사업부장")

        assertThat(resolver.resolve(employee)).isEqualTo(ProfileType.BUSINESS_DIRECTOR)
    }

    @Test
    @DisplayName("[Branch 7] jikchak == '실장' (contains '부장' false) → SALES_REP (default)")
    fun siljangIsSalesRepBecauseNotBujang() {
        // '실장' 자체는 contains('부장') 매칭 안 됨 → default
        stubOrg(costCenterCode = "1100", orgCodeLevel3 = "1100")
        val employee = createEmployee(costCenterCode = "1100", jikchak = "실장")

        assertThat(resolver.resolve(employee)).isEqualTo(ProfileType.SALES_REP)
    }

    @Test
    @DisplayName("[Branch 8] jikchak == '본부장' → DIVISION_HEAD (contains '부장' 매칭되나 사업/실장 아님 → 본부장 분기)")
    fun divisionHead() {
        stubOrg(costCenterCode = "1100", orgCodeLevel3 = "1100")
        val employee = createEmployee(costCenterCode = "1100", jikchak = "본부장")

        assertThat(resolver.resolve(employee)).isEqualTo(ProfileType.DIVISION_HEAD)
    }

    @Test
    @DisplayName("[Branch 9] jikchak contains '부장' (그 외) → SALES_MANAGER")
    fun salesManagerByBujang() {
        stubOrg(costCenterCode = "1100", orgCodeLevel3 = "1100")
        val employee = createEmployee(costCenterCode = "1100", jikchak = "영업부장")

        assertThat(resolver.resolve(employee)).isEqualTo(ProfileType.SALES_MANAGER)
    }

    @Test
    @DisplayName("[Branch 10 default] 그 외 → SALES_REP")
    fun salesRepDefault() {
        stubOrg(costCenterCode = "1100", orgCodeLevel3 = "1100")
        val employee = createEmployee(costCenterCode = "1100", jikchak = "사원")

        assertThat(resolver.resolve(employee)).isEqualTo(ProfileType.SALES_REP)
    }

    @Test
    @DisplayName("[Edge] Organization cascade lookup 실패 → STAFF 디폴트")
    fun orgLookupAllMissing() {
        // cascade Level5→4→3 모두 miss → null. Resolver 는 STAFF 디폴트.
        whenever(organizationRepository.findFirstByOrgCodeCascade("9999")).thenReturn(null)
        val employee = createEmployee(costCenterCode = "9999", jikchak = "사업부장")

        assertThat(resolver.resolve(employee)).isEqualTo(ProfileType.STAFF)
    }

    @Test
    @DisplayName("[Edge] costCenterCode null → STAFF 디폴트 (cascade lookup 즉시 null)")
    fun costCenterCodeNull() {
        val employee = createEmployee(costCenterCode = null, jikchak = "사업부장")

        assertThat(resolver.resolve(employee)).isEqualTo(ProfileType.STAFF)
    }

    @Test
    @DisplayName("[Edge] jikchak null + Branch 5~9 매칭 실패 → SALES_REP default")
    fun jikchakNull() {
        stubOrg(costCenterCode = "1100", orgCodeLevel3 = "1100")
        val employee = createEmployee(costCenterCode = "1100", jikchak = null)

        assertThat(resolver.resolve(employee)).isEqualTo(ProfileType.SALES_REP)
    }

    @Test
    @DisplayName("[Cascade] cascade lookup 결과 (Level3 hit 등) — resolver 는 cascade 메커니즘 비인지")
    fun cascadeLevel4Hit() {
        // cascade 메커니즘 (Level5/4/3 순서) 자체는 OrganizationRepositoryCustomImpl 의 책임.
        // resolver 테스트는 "cascade 가 Org 를 돌려주면 정책 분기가 의도대로 평가되는가" 만 검증.
        whenever(organizationRepository.findFirstByOrgCodeCascade("5066")).thenReturn(
            organization(orgCodeLevel3 = "5066")
        )
        val employee = createEmployee(costCenterCode = "5066", jikchak = "사원")

        assertThat(resolver.resolve(employee)).isEqualTo(ProfileType.MARKETING)
    }

    private fun stubOrg(costCenterCode: String, orgCodeLevel3: String?) {
        whenever(organizationRepository.findFirstByOrgCodeCascade(costCenterCode))
            .thenReturn(organization(orgCodeLevel3 = orgCodeLevel3))
    }

    private fun organization(orgCodeLevel3: String?): Organization = Organization(orgCodeLevel3 = orgCodeLevel3)

    private fun createEmployee(costCenterCode: String?, jikchak: String?): Employee = Employee(
        employeeCode = "100234",
        name = "테스트사원",
        costCenterCode = costCenterCode,
        jikchak = jikchak
    )
}
