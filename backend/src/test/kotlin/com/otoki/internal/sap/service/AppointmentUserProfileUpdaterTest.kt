package com.otoki.internal.sap.service

import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.sap.entity.Appointment
import com.otoki.internal.sap.entity.Organization
import com.otoki.internal.sap.repository.OrganizationRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("AppointmentUserProfileUpdater 테스트")
class AppointmentUserProfileUpdaterTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var organizationRepository: OrganizationRepository

    @InjectMocks
    private lateinit var updater: AppointmentUserProfileUpdater

    @Nested
    @DisplayName("determineAuthority - 프로필 결정 로직")
    inner class DetermineAuthorityTests {

        @Test
        @DisplayName("마케팅실 소속 - orgCodeLevel3=5066 -> 마케팅")
        fun marketing() {
            val result = updater.determineAuthority("5066", "1234", "사원")
            assertThat(result).isEqualTo("마케팅")
        }

        @Test
        @DisplayName("지원실 일반직 - orgCodeLevel3=3475, 사원 -> Staff")
        fun supportStaff() {
            val result = updater.determineAuthority("3475", "1234", "사원")
            assertThat(result).isEqualTo("Staff")
        }

        @Test
        @DisplayName("지원실 조장 - orgCodeLevel3=3475, 조장 -> 조장")
        fun supportLeader() {
            val result = updater.determineAuthority("3475", "1234", "조장")
            assertThat(result).isEqualTo("조장")
        }

        @Test
        @DisplayName("지원실 판매 - orgCodeLevel3=3475, 판매 -> 직책 기반")
        fun supportSales() {
            val result = updater.determineAuthority("3475", "1234", "판매")
            assertThat(result).isEqualTo("영업사원")
        }

        @Test
        @DisplayName("판매전략팀 - orgCodeLevel3=3472 -> Staff")
        fun salesStrategy() {
            val result = updater.determineAuthority("3472", "1234", "사원")
            assertThat(result).isEqualTo("Staff")
        }

        @Test
        @DisplayName("BS팀 5397 -> Staff")
        fun bsTeam5397() {
            val result = updater.determineAuthority("9999", "5397", "사원")
            assertThat(result).isEqualTo("Staff")
        }

        @Test
        @DisplayName("BS팀 5398 -> Staff")
        fun bsTeam5398() {
            val result = updater.determineAuthority("9999", "5398", "사원")
            assertThat(result).isEqualTo("Staff")
        }

        @Test
        @DisplayName("SP팀 5639 -> Staff")
        fun spTeam5639() {
            val result = updater.determineAuthority("9999", "5639", "사원")
            assertThat(result).isEqualTo("Staff")
        }

        @Test
        @DisplayName("조장 -> 조장")
        fun leader() {
            val result = updater.determineAuthority("9999", "1234", "조장")
            assertThat(result).isEqualTo("조장")
        }

        @Test
        @DisplayName("판매팀장 -> 조장")
        fun salesTeamLeader() {
            val result = updater.determineAuthority("9999", "1234", "판매팀장")
            assertThat(result).isEqualTo("조장")
        }

        @Test
        @DisplayName("지점장 -> 지점장")
        fun branchManager() {
            val result = updater.determineAuthority("9999", "1234", "지점장")
            assertThat(result).isEqualTo("지점장")
        }

        @Test
        @DisplayName("팀장 -> 지점장")
        fun teamManager() {
            val result = updater.determineAuthority("9999", "1234", "팀장")
            assertThat(result).isEqualTo("지점장")
        }

        @Test
        @DisplayName("영업부장 -> 영업부장")
        fun salesDirector() {
            val result = updater.determineAuthority("9999", "1234", "부장")
            assertThat(result).isEqualTo("영업부장")
        }

        @Test
        @DisplayName("사업부장 -> 사업부장")
        fun businessDirector() {
            val result = updater.determineAuthority("9999", "1234", "사업부장")
            assertThat(result).isEqualTo("사업부장")
        }

        @Test
        @DisplayName("실장 -> 사업부장")
        fun divisionHead() {
            val result = updater.determineAuthority("9999", "1234", "실장")
            assertThat(result).isEqualTo("영업사원")
        }

        @Test
        @DisplayName("본부장 -> 영업본부장")
        fun headquartersHead() {
            val result = updater.determineAuthority("9999", "1234", "본부장")
            assertThat(result).isEqualTo("영업본부장")
        }

        @Test
        @DisplayName("일반 사원 -> 영업사원")
        fun generalEmployee() {
            val result = updater.determineAuthority("9999", "1234", "사원")
            assertThat(result).isEqualTo("영업사원")
        }

        @Test
        @DisplayName("jikchak null 처리 -> 영업사원")
        fun nullJikchak() {
            val result = updater.determineAuthority("9999", "1234", "")
            assertThat(result).isEqualTo("영업사원")
        }
    }

    @Nested
    @DisplayName("updateUserProfiles - 후처리 흐름")
    inner class UpdateUserProfilesTests {

        @Test
        @DisplayName("정상 갱신 - Org 매칭, User 존재 -> appAuthority, costCenterCode, orgName 갱신")
        fun normalUpdate() {
            val org = createOrg(costCenterLevel5 = "1111", orgCodeLevel3 = "5066")
            whenever(organizationRepository.findAll()).thenReturn(listOf(org))

            val user = createUser(employeeId = "100234")
            whenever(userRepository.findByEmployeeId("100234")).thenReturn(Optional.of(user))

            val appointment = createAppointment(
                employeeCode = "100234",
                empCodeExist = true,
                afterOrgCode = "1111",
                afterOrgName = "마케팅1팀",
                jikchak = "사원"
            )

            updater.updateUserProfiles(listOf(appointment))

            assertThat(user.appAuthority).isEqualTo("마케팅")
            assertThat(user.costCenterCode).isEqualTo("1111")
            assertThat(user.orgName).isEqualTo("마케팅1팀")
        }

        @Test
        @DisplayName("User 미존재 - empCodeExist=false -> 건너뜀")
        fun skipWhenEmpCodeExistFalse() {
            whenever(organizationRepository.findAll()).thenReturn(emptyList())

            val appointment = createAppointment(
                employeeCode = "100234",
                empCodeExist = false,
                afterOrgCode = "1111"
            )

            updater.updateUserProfiles(listOf(appointment))
            // no exception, just skipped
        }

        @Test
        @DisplayName("afterOrgCode null -> 건너뜀")
        fun skipWhenAfterOrgCodeNull() {
            whenever(organizationRepository.findAll()).thenReturn(emptyList())

            val appointment = createAppointment(
                employeeCode = "100234",
                empCodeExist = true,
                afterOrgCode = null
            )

            updater.updateUserProfiles(listOf(appointment))
        }

        @Test
        @DisplayName("Org 미존재 - appAuthority 미변경, costCenterCode/orgName은 갱신")
        fun orgNotFound() {
            whenever(organizationRepository.findAll()).thenReturn(emptyList())

            val user = createUser(employeeId = "100234", appAuthority = "영업사원")
            whenever(userRepository.findByEmployeeId("100234")).thenReturn(Optional.of(user))

            val appointment = createAppointment(
                employeeCode = "100234",
                empCodeExist = true,
                afterOrgCode = "9999",
                afterOrgName = "신규조직"
            )

            updater.updateUserProfiles(listOf(appointment))

            assertThat(user.appAuthority).isEqualTo("영업사원")
            assertThat(user.costCenterCode).isEqualTo("9999")
            assertThat(user.orgName).isEqualTo("신규조직")
        }

        @Test
        @DisplayName("costCenterLevel4 매칭 - Level5 없으면 Level4로 검색")
        fun matchByCostCenterLevel4() {
            val org = createOrg(costCenterLevel4 = "2222", orgCodeLevel3 = "3472")
            whenever(organizationRepository.findAll()).thenReturn(listOf(org))

            val user = createUser(employeeId = "100234")
            whenever(userRepository.findByEmployeeId("100234")).thenReturn(Optional.of(user))

            val appointment = createAppointment(
                employeeCode = "100234",
                empCodeExist = true,
                afterOrgCode = "2222",
                jikchak = "사원"
            )

            updater.updateUserProfiles(listOf(appointment))

            assertThat(user.appAuthority).isEqualTo("Staff")
        }

        @Test
        @DisplayName("복수 발령 - 마지막 발령이 최종 반영")
        fun multipleAppointments() {
            val org1 = createOrg(costCenterLevel5 = "1111", orgCodeLevel3 = "5066")
            val org2 = createOrg(costCenterLevel5 = "2222", orgCodeLevel3 = "9999")
            whenever(organizationRepository.findAll()).thenReturn(listOf(org1, org2))

            val user = createUser(employeeId = "100234")
            whenever(userRepository.findByEmployeeId("100234")).thenReturn(Optional.of(user))

            val appointments = listOf(
                createAppointment(
                    employeeCode = "100234", empCodeExist = true,
                    afterOrgCode = "1111", afterOrgName = "마케팅", jikchak = "사원"
                ),
                createAppointment(
                    employeeCode = "100234", empCodeExist = true,
                    afterOrgCode = "2222", afterOrgName = "영업1지점", jikchak = "지점장"
                )
            )

            updater.updateUserProfiles(appointments)

            assertThat(user.appAuthority).isEqualTo("지점장")
            assertThat(user.orgName).isEqualTo("영업1지점")
        }
    }

    private fun createUser(
        id: Long = 1L,
        employeeId: String = "100234",
        appAuthority: String? = null
    ): User = User(
        id = id,
        employeeId = employeeId,
        name = "테스트사원",
        appAuthority = appAuthority
    )

    private fun createOrg(
        id: Long = 0L,
        costCenterLevel3: String? = null,
        orgCodeLevel3: String? = null,
        costCenterLevel4: String? = null,
        orgCodeLevel4: String? = null,
        costCenterLevel5: String? = null,
        orgCodeLevel5: String? = null
    ): Organization = Organization(
        id = id,
        costCenterLevel3 = costCenterLevel3,
        orgCodeLevel3 = orgCodeLevel3,
        costCenterLevel4 = costCenterLevel4,
        orgCodeLevel4 = orgCodeLevel4,
        costCenterLevel5 = costCenterLevel5,
        orgCodeLevel5 = orgCodeLevel5
    )

    private fun createAppointment(
        employeeCode: String = "100234",
        empCodeExist: Boolean = true,
        afterOrgCode: String? = null,
        afterOrgName: String? = null,
        jikchak: String? = null
    ): Appointment = Appointment(
        employeeCode = employeeCode,
        empCodeExist = empCodeExist,
        afterOrgCode = afterOrgCode,
        afterOrgName = afterOrgName,
        jikchak = jikchak,
        appointDate = "20260301"
    )
}
