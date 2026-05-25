package com.otoki.powersales.user.service

import com.otoki.powersales.auth.entity.Profile
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.organization.repository.OrganizationRepository
import com.otoki.powersales.organization.repository.dto.OrganizationCacheDto
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("EmployeeProfileResolver — SF AppointmentTriggerHandler 10개 분기 동등 검증 (Profile.id 산출)")
class EmployeeProfileResolverTest {

    private val organizationRepository: OrganizationRepository = mockk()
    private val profileRepository: com.otoki.powersales.auth.repository.ProfileRepository = mockk()

    private val resolver = EmployeeProfileResolver(
        organizationRepository,
        profileRepository,
    )

    init {
        // Profile.name → id stub (모든 12종 가용).
        stubProfile(101L, "8.마케팅")
        stubProfile(102L, "9. Staff")
        stubProfile(103L, "6.조장")
        stubProfile(104L, "4.지점장")
        stubProfile(105L, "3.영업부장")
        stubProfile(106L, "2.사업부장")
        stubProfile(107L, "1.본부장")
        stubProfile(108L, "5.영업사원")
    }

    @Test
    @DisplayName("[Branch 1] orgCodeLevel3=='5066' (마케팅실) → 8.마케팅 — 직책 무관")
    fun marketing() {
        stubOrg(costCenterCode = "5066", orgCodeLevel3 = "5066")
        val employee = createEmployee(costCenterCode = "5066", jikchak = "사원")

        assertThat(resolver.resolveProfileId(employee)).isEqualTo(101L)
    }

    @Test
    @DisplayName("[Branch 2] orgCodeLevel3=='3475' (지원실) + 판매/조장 아님 → 9. Staff")
    fun supportStaff() {
        stubOrg(costCenterCode = "3475", orgCodeLevel3 = "3475")
        val employee = createEmployee(costCenterCode = "3475", jikchak = "사원")

        assertThat(resolver.resolveProfileId(employee)).isEqualTo(102L)
    }

    @Test
    @DisplayName("[Branch 2 negative] orgCodeLevel3=='3475' + 판매팀장 → STAFF 매칭 안 됨, fall-through → 6.조장")
    fun supportSalesTeamLeader() {
        stubOrg(costCenterCode = "3475", orgCodeLevel3 = "3475")
        val employee = createEmployee(costCenterCode = "3475", jikchak = "판매팀장")

        // Branch 2 (STAFF) 매칭 안 됨 → Branch 5 (TEAM_LEADER) 매칭
        assertThat(resolver.resolveProfileId(employee)).isEqualTo(103L)
    }

    @Test
    @DisplayName("[Branch 3] orgCodeLevel3=='3472' (판매전략팀) → 9. Staff — 직책 무관")
    fun salesStrategyStaff() {
        stubOrg(costCenterCode = "3472", orgCodeLevel3 = "3472")
        val employee = createEmployee(costCenterCode = "3472", jikchak = "지점장")

        assertThat(resolver.resolveProfileId(employee)).isEqualTo(102L)
    }

    @Test
    @DisplayName("[Branch 4] costCenterCode in {5397, 5398, 5639} (BS팀/SP팀) → 9. Staff")
    fun bsSpTeamStaff() {
        stubOrg(costCenterCode = "5397", orgCodeLevel3 = "1100")
        val employee = createEmployee(costCenterCode = "5397", jikchak = "팀장")

        assertThat(resolver.resolveProfileId(employee)).isEqualTo(102L)
    }

    @Test
    @DisplayName("[Branch 5] jikchak contains '조장' → 6.조장")
    fun teamLeaderByJikchak() {
        stubOrg(costCenterCode = "1100", orgCodeLevel3 = "1100")
        val employee = createEmployee(costCenterCode = "1100", jikchak = "판매조장")

        assertThat(resolver.resolveProfileId(employee)).isEqualTo(103L)
    }

    @Test
    @DisplayName("[Branch 6] jikchak contains '지점장' → 4.지점장")
    fun branchManagerByJikchak() {
        stubOrg(costCenterCode = "1100", orgCodeLevel3 = "1100")
        val employee = createEmployee(costCenterCode = "1100", jikchak = "지점장직무대행")

        assertThat(resolver.resolveProfileId(employee)).isEqualTo(104L)
    }

    @Test
    @DisplayName("[Branch 7] jikchak contains '부장' + '사업' → 2.사업부장")
    fun businessDirector() {
        stubOrg(costCenterCode = "1100", orgCodeLevel3 = "1100")
        val employee = createEmployee(costCenterCode = "1100", jikchak = "사업부장")

        assertThat(resolver.resolveProfileId(employee)).isEqualTo(106L)
    }

    @Test
    @DisplayName("[Branch 7] jikchak == '실장' (contains '부장' false) → 5.영업사원 (default)")
    fun siljangIsSalesRepBecauseNotBujang() {
        stubOrg(costCenterCode = "1100", orgCodeLevel3 = "1100")
        val employee = createEmployee(costCenterCode = "1100", jikchak = "실장")

        assertThat(resolver.resolveProfileId(employee)).isEqualTo(108L)
    }

    @Test
    @DisplayName("[Branch 8] jikchak == '본부장' → 1.본부장")
    fun divisionHead() {
        stubOrg(costCenterCode = "1100", orgCodeLevel3 = "1100")
        val employee = createEmployee(costCenterCode = "1100", jikchak = "본부장")

        assertThat(resolver.resolveProfileId(employee)).isEqualTo(107L)
    }

    @Test
    @DisplayName("[Branch 9] jikchak contains '부장' (그 외) → 3.영업부장")
    fun salesManagerByBujang() {
        stubOrg(costCenterCode = "1100", orgCodeLevel3 = "1100")
        val employee = createEmployee(costCenterCode = "1100", jikchak = "영업부장")

        assertThat(resolver.resolveProfileId(employee)).isEqualTo(105L)
    }

    @Test
    @DisplayName("[Branch 10 default] 그 외 → 5.영업사원")
    fun salesRepDefault() {
        stubOrg(costCenterCode = "1100", orgCodeLevel3 = "1100")
        val employee = createEmployee(costCenterCode = "1100", jikchak = "사원")

        assertThat(resolver.resolveProfileId(employee)).isEqualTo(108L)
    }

    @Test
    @DisplayName("[Edge] Organization cascade lookup 실패 → 9. Staff 디폴트")
    fun orgLookupAllMissing() {
        every { organizationRepository.findFirstByOrgCodeCascade("9999") } returns null
        val employee = createEmployee(costCenterCode = "9999", jikchak = "사업부장")

        assertThat(resolver.resolveProfileId(employee)).isEqualTo(102L)
    }

    @Test
    @DisplayName("[Edge] costCenterCode null → 9. Staff 디폴트 (cascade lookup 즉시 null)")
    fun costCenterCodeNull() {
        val employee = createEmployee(costCenterCode = null, jikchak = "사업부장")

        assertThat(resolver.resolveProfileId(employee)).isEqualTo(102L)
    }

    @Test
    @DisplayName("[Edge] jikchak null + Branch 5~9 매칭 실패 → 5.영업사원 default")
    fun jikchakNull() {
        stubOrg(costCenterCode = "1100", orgCodeLevel3 = "1100")
        val employee = createEmployee(costCenterCode = "1100", jikchak = null)

        assertThat(resolver.resolveProfileId(employee)).isEqualTo(108L)
    }

    @Test
    @DisplayName("[Cascade] cascade lookup 결과 (Level3 hit 등) — resolver 는 cascade 메커니즘 비인지")
    fun cascadeLevel4Hit() {
        every { organizationRepository.findFirstByOrgCodeCascade("5066") } returns
            organization(orgCodeLevel3 = "5066")
        val employee = createEmployee(costCenterCode = "5066", jikchak = "사원")

        assertThat(resolver.resolveProfileId(employee)).isEqualTo(101L)
    }

    @Test
    @DisplayName("[Edge] Profile entity 부재 → null 반환 (Stage1 적재 / LocalDataInitializer 시드 누락 케이스)")
    fun profileMissing() {
        stubOrg(costCenterCode = "5066", orgCodeLevel3 = "5066")
        every { profileRepository.findByName("8.마케팅") } returns null
        val employee = createEmployee(costCenterCode = "5066", jikchak = "사원")

        assertThat(resolver.resolveProfileId(employee)).isNull()
    }

    private fun stubOrg(costCenterCode: String, orgCodeLevel3: String?) {
        every { organizationRepository.findFirstByOrgCodeCascade(costCenterCode) } returns
            organization(orgCodeLevel3 = orgCodeLevel3)
    }

    private fun stubProfile(id: Long, name: String) {
        every { profileRepository.findByName(name) } returns Profile(id = id, name = name)
    }

    private fun organization(orgCodeLevel3: String?): OrganizationCacheDto =
        OrganizationCacheDto(
            orgCodeLevel3 = orgCodeLevel3,
            orgNameLevel3 = null,
            orgNameLevel4 = null,
            costCenterLevel3 = null,
        )

    private fun createEmployee(costCenterCode: String?, jikchak: String?): Employee = Employee(
        employeeCode = "100234",
        name = "테스트사원",
        costCenterCode = costCenterCode,
        jikchak = jikchak
    )
}
