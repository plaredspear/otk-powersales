package com.otoki.powersales.user.service

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.domain.org.organization.repository.dto.OrganizationCacheDto
import com.otoki.powersales.user.entity.User
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("UserOrgDisplayFieldsSynchronizer 테스트")
class UserOrgDisplayFieldsSynchronizerTest {

    private val organizationRepository: OrganizationRepository = mockk(relaxed = true)
    private val synchronizer = UserOrgDisplayFieldsSynchronizer(organizationRepository)

    private fun employee(
        costCenterCode: String? = "5826",
        orgName: String? = "인천1지점",
        jikwee: String? = "사원",
    ): Employee = Employee(employeeCode = "100234", name = "테스트사원").apply {
        this.costCenterCode = costCenterCode
        this.orgName = orgName
        this.jikwee = jikwee
    }

    private fun user(): User = User(
        username = "u@otoki.local",
        employeeCode = "100234",
        password = "x",
    )

    private fun org(level3: String? = "제1사업부", level4: String? = "1영업부") = OrganizationCacheDto(
        orgCodeLevel3 = "3000",
        orgNameLevel3 = level3,
        orgNameLevel4 = level4,
        costCenterLevel3 = "3000",
    )

    @Nested
    @DisplayName("sync - SF AppointmentTriggerHanlder cls:313-323 정합")
    inner class Sync {

        @Test
        @DisplayName("조직 lookup 성공 - branch/division/department/title/hrCode 갱신 후 true")
        fun syncsAllDisplayFields() {
            val user = user()
            every { organizationRepository.findFirstByOrgCodeCascade("5826") } returns org()

            assertThat(synchronizer.sync(user, employee())).isTrue()

            assertThat(user.branch).isEqualTo("인천1지점")
            assertThat(user.division).isEqualTo("제1사업부")
            assertThat(user.department).isEqualTo("제1사업부_1영업부")
            assertThat(user.title).isEqualTo("사원")
            assertThat(user.hrCode).isEqualTo("5826")
        }

        @Test
        @DisplayName("조직 lookup 실패 - 전 필드 무변경 후 false (SF orgInfoTmp == null 가드)")
        fun keepsFieldsWhenLookupFails() {
            val user = user().apply { branch = "기존지점"; division = "기존사업부" }
            every { organizationRepository.findFirstByOrgCodeCascade("5826") } returns null

            assertThat(synchronizer.sync(user, employee())).isFalse()

            assertThat(user.branch).isEqualTo("기존지점")
            assertThat(user.division).isEqualTo("기존사업부")
            assertThat(user.department).isNull()
            assertThat(user.hrCode).isNull()
        }

        @Test
        @DisplayName("costCenterCode 부재 - 조직 조회 없이 false")
        fun returnsFalseWithoutCostCenterCode() {
            val user = user()

            assertThat(synchronizer.sync(user, employee(costCenterCode = null))).isFalse()
            assertThat(synchronizer.sync(user, employee(costCenterCode = " "))).isFalse()
            assertThat(user.branch).isNull()
        }

        @Test
        @DisplayName("발령 이력 없는 사원 - orgName 부재라 branch 는 null, 나머지 4개는 채워짐")
        fun leavesBranchNullWithoutOrgName() {
            val user = user()
            every { organizationRepository.findFirstByOrgCodeCascade("5826") } returns org()

            assertThat(synchronizer.sync(user, employee(orgName = null))).isTrue()

            assertThat(user.branch).isNull()
            // catch-up 재조회 마커가 branch 단독이면 이 행이 매 부팅 재처리된다 — division 이 마커인 이유.
            assertThat(user.division).isEqualTo("제1사업부")
        }

        @Test
        @DisplayName("profileId / isSalesSupport 는 건드리지 않는다 (수동 프로파일 지정 보호)")
        fun doesNotTouchPermissionDerivedColumns() {
            val user = user().apply { profileId = 42L; isSalesSupport = true }
            every { organizationRepository.findFirstByOrgCodeCascade("5826") } returns org()

            synchronizer.sync(user, employee())

            assertThat(user.profileId).isEqualTo(42L)
            assertThat(user.isSalesSupport).isTrue()
        }
    }

    @Nested
    @DisplayName("joinDepartment - SF cls:316 문자열 조립")
    inner class JoinDepartment {

        @Test
        @DisplayName("레벨명이 비면 리터럴 \"null\" 대신 제외 (Apex 문자열 연결 결함 미재현)")
        fun skipsBlankLevels() {
            assertThat(synchronizer.joinDepartment("제1사업부", "1영업부")).isEqualTo("제1사업부_1영업부")
            assertThat(synchronizer.joinDepartment("제1사업부", null)).isEqualTo("제1사업부")
            assertThat(synchronizer.joinDepartment(null, "1영업부")).isEqualTo("1영업부")
            assertThat(synchronizer.joinDepartment("제1사업부", " ")).isEqualTo("제1사업부")
            assertThat(synchronizer.joinDepartment(null, null)).isNull()
        }
    }
}
