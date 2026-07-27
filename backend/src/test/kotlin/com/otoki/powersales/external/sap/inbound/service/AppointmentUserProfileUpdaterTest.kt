package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.admin.security.AdminDataScopeCache
import com.otoki.powersales.platform.auth.permission.AdminPermissionCache
import com.otoki.powersales.platform.common.entity.SystemCodeMaster
import com.otoki.powersales.platform.common.repository.SystemCodeMasterRepository
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.activity.schedule.entity.Appointment
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import com.otoki.powersales.user.service.EmployeeProfileResolver
import com.otoki.powersales.user.service.UserRoleResolver
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional

@DisplayName("AppointmentUserProfileUpdater 테스트")
class AppointmentUserProfileUpdaterTest {

    private val employeeRepository: EmployeeRepository = mockk(relaxed = true)
    private val organizationRepository: OrganizationRepository = mockk(relaxed = true)
    private val systemCodeMasterRepository: SystemCodeMasterRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val employeeProfileResolver: EmployeeProfileResolver = mockk(relaxed = true)
    private val userRoleResolver: UserRoleResolver = mockk(relaxed = true)
    private val adminPermissionCache: AdminPermissionCache = mockk(relaxed = true)
    private val adminDataScopeCache: AdminDataScopeCache = mockk(relaxed = true)
    private val updater = AppointmentUserProfileUpdater(
        employeeRepository,
        organizationRepository,
        systemCodeMasterRepository,
        userRepository,
        employeeProfileResolver,
        userRoleResolver,
        adminPermissionCache,
        adminDataScopeCache,
    )

    private val today = LocalDate.of(2026, 3, 22)

    private fun setupSystemCodeMaster() {
        every {
            systemCodeMasterRepository.findByGroupCodeIn(
                listOf("H20020", "H20030", "H20010", "H10050", "H10060")
            )
        } returns listOf(
            createSystemCodeMaster("H20020", "D0052", "조장"),
            createSystemCodeMaster("H20020", "D0053", "사원"),
            createSystemCodeMaster("H20030", "W0010", "사원"),
            createSystemCodeMaster("H20010", "G0030", "3급"),
            createSystemCodeMaster("H10050", "T0010", "영업직"),
            createSystemCodeMaster("H10060", "A055", "OSC직"),
            createSystemCodeMaster("H10060", "A049", "판촉직"),
            createSystemCodeMaster("H10060", "A053", "레이디직"),
            createSystemCodeMaster("H10060", "B001", "일반직")
        )
    }

    @Nested
    @DisplayName("즉시 반영 - 발령일 <= 오늘")
    inner class ImmediateAppointmentTests {

        @BeforeEach
        fun setUp() {
            setupSystemCodeMaster()
        }

        @Test
        @DisplayName("조장 - jobCode=A055, jikchak=D0052 -> appAuthority=조장, appLoginActive=true")
        fun immediateLeader() {
            val employee = createEmployee()
            every { employeeRepository.findByEmployeeCode("100234") } returns Optional.of(employee)

            val appointment = createAppointment(
                afterOrgCode = "1111", afterOrgName = "제1영업지점",
                jikchak = "D0052", jikwee = "W0010", jikgub = "G0030",
                workType = "T0010", jobCode = "A055",
                workArea = "서울", jikjong = "영업",
                appointDate = LocalDate.of(2026, 3, 22), ordDetailNode = "전보"
            )

            updater.updateUserProfiles(listOf(appointment), today)

            assertThat(employee.role).isEqualTo(AppAuthority.LEADER)
            assertThat(employee.appLoginActive).isTrue()
            assertThat(employee.jikchak).isEqualTo("조장")
            assertThat(employee.jikwee).isEqualTo("사원")
            assertThat(employee.jikgub).isEqualTo("3급")
            assertThat(employee.workType).isEqualTo("영업직")
            assertThat(employee.jobCode).isEqualTo("OSC직")
            assertThat(employee.workArea).isEqualTo("서울")
            assertThat(employee.jikjong).isEqualTo("영업")
            assertThat(employee.appointmentDate).isEqualTo(today)
            assertThat(employee.ordDetailNode).isEqualTo("전보")
            assertThat(employee.costCenterCode).isEqualTo("1111")
            assertThat(employee.orgName).isEqualTo("제1영업지점")
        }

        @Test
        @DisplayName("즉시 반영 - 기존 예약 필드는 건드리지 않음 (SF cls:131-178 — upsert 에 예약 필드 미포함)")
        fun immediateKeepsExistingReservation() {
            // 미소진 예약 보유 중 별개 즉시 발령 수신 — SF 는 예약을 유지해 발령일에 배치가 반영한다.
            val pendingReservation = createAppointment(
                afterOrgCode = "9999", afterOrgName = "예약지점",
                jobCode = "A049", appointDate = LocalDate.of(2026, 4, 1)
            )
            val employee = createEmployee().apply {
                crmWorkStartDate = LocalDate.of(2026, 4, 1)
                postponedAppointment = pendingReservation
            }
            every { employeeRepository.findByEmployeeCode("100234") } returns Optional.of(employee)

            val immediate = createAppointment(
                afterOrgCode = "1111", afterOrgName = "제1영업지점",
                jikchak = "D0053", jobCode = "A049",
                appointDate = LocalDate.of(2026, 3, 22), ordDetailNode = "전보"
            )

            updater.updateUserProfiles(listOf(immediate), today)

            assertThat(employee.orgName).isEqualTo("제1영업지점")
            assertThat(employee.crmWorkStartDate).isEqualTo(LocalDate.of(2026, 4, 1))
            assertThat(employee.postponedAppointment).isSameAs(pendingReservation)
        }

        @Test
        @DisplayName("여사원 - jobCode=A049, jikchak!=D0052, ordDetailNode=전보 -> 여사원, professionalPromotionTeam=일반")
        fun immediateWorker() {
            val employee = createEmployee()
            every { employeeRepository.findByEmployeeCode("100234") } returns Optional.of(employee)

            val appointment = createAppointment(
                afterOrgCode = "1111", afterOrgName = "제2영업지점",
                jikchak = "D0053", jobCode = "A049",
                appointDate = LocalDate.of(2026, 3, 22), ordDetailNode = "전보"
            )

            updater.updateUserProfiles(listOf(appointment), today)

            assertThat(employee.role).isEqualTo(AppAuthority.WOMAN)
            assertThat(employee.appLoginActive).isTrue()
            assertThat(employee.professionalPromotionTeam).isNull()
        }

        @Test
        @DisplayName("일반직 - jobCode=B001 -> role/appLoginActive 변경 없음")
        fun immediateGeneral() {
            val employee = createEmployee(role = null, appLoginActive = false)
            every { employeeRepository.findByEmployeeCode("100234") } returns Optional.of(employee)

            val appointment = createAppointment(
                afterOrgCode = "1111", afterOrgName = "본사",
                jikchak = "D0053", jobCode = "B001",
                appointDate = LocalDate.of(2026, 3, 22)
            )

            updater.updateUserProfiles(listOf(appointment), today)

            assertThat(employee.role).isEqualTo(null)
            assertThat(employee.appLoginActive).isFalse()
            assertThat(employee.costCenterCode).isEqualTo("1111")
            assertThat(employee.jikchak).isEqualTo("사원")
            assertThat(employee.jobCode).isEqualTo("일반직")
        }

        @Test
        @DisplayName("승진 발령 - ordDetailNode=승진 -> professionalPromotionTeam 기존 값 유지")
        fun promotionKeepsPPT() {
            val employee = createEmployee(professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)
            every { employeeRepository.findByEmployeeCode("100234") } returns Optional.of(employee)

            val appointment = createAppointment(
                afterOrgCode = "1111", afterOrgName = "지점",
                jikchak = "D0053", jobCode = "A055",
                appointDate = LocalDate.of(2026, 3, 22), ordDetailNode = "승진"
            )

            updater.updateUserProfiles(listOf(appointment), today)

            assertThat(employee.role).isEqualTo(AppAuthority.WOMAN)
            assertThat(employee.professionalPromotionTeam).isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
        }
    }

    @Nested
    @DisplayName("조직명 특수규칙")
    inner class OrgNameSpecialRulesTests {

        @Test
        @DisplayName("유통총괄1부 - afterOrgCode=3228 -> 유통총괄1부+afterOrgName")
        fun orgGroup1() {
            val result = updater.resolveOrgName("3228", "제1영업지점")
            assertThat(result).isEqualTo("유통총괄1부제1영업지점")
        }

        @Test
        @DisplayName("유통총괄2부 - afterOrgCode=3233 -> 유통총괄2부+afterOrgName")
        fun orgGroup2() {
            val result = updater.resolveOrgName("3233", "제2영업지점")
            assertThat(result).isEqualTo("유통총괄2부제2영업지점")
        }

        @Test
        @DisplayName("일반 조직 - 그 외 코드 -> afterOrgName 그대로")
        fun orgGeneral() {
            val result = updater.resolveOrgName("9999", "일반지점")
            assertThat(result).isEqualTo("일반지점")
        }
    }

    @Nested
    @DisplayName("SystemCodeMaster 코드 변환")
    inner class CodeResolutionTests {

        @Test
        @DisplayName("코드 변환 성공 - 매칭 존재 -> detailCodeName 반환")
        fun codeResolutionSuccess() {
            val codeMap = mapOf("H20020:D0052" to "조장")
            val result = updater.resolveCode(codeMap, "H20020", "D0052")
            assertThat(result).isEqualTo("조장")
        }

        @Test
        @DisplayName("코드 변환 실패 - 매칭 없음 -> null 반환 (SF 정합 — 원시코드 저장 금지)")
        fun codeResolutionMissReturnsNull() {
            val codeMap = mapOf("H20020:D0052" to "조장")
            val result = updater.resolveCode(codeMap, "H20020", "UNKNOWN")
            assertThat(result).isNull()
        }

        @Test
        @DisplayName("null 코드 -> null 반환")
        fun nullCode() {
            val codeMap = mapOf("H20020:D0052" to "조장")
            val result = updater.resolveCode(codeMap, "H20020", null)
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("예약 발령 - 발령일 > 오늘")
    inner class ReservedAppointmentTests {

        @BeforeEach
        fun setUp() {
            setupSystemCodeMaster()
        }

        @Test
        @DisplayName("예약 발령 - crmWorkStartDate 설정, AppAuthority 즉시, 나머지 미변경")
        fun reservedAppointment() {
            val employee = createEmployee(orgName = "기존지점", costCenterCode = "0000")
            every { employeeRepository.findByEmployeeCode("100234") } returns Optional.of(employee)

            val appointment = createAppointment(
                afterOrgCode = "1111", afterOrgName = "신규지점",
                jikchak = "D0052", jobCode = "A055",
                appointDate = LocalDate.of(2026, 3, 23)
            )

            updater.updateUserProfiles(listOf(appointment), today)

            assertThat(employee.crmWorkStartDate).isEqualTo(LocalDate.of(2026, 3, 23))
            assertThat(employee.role).isEqualTo(AppAuthority.LEADER)
            assertThat(employee.appLoginActive).isTrue()
            assertThat(employee.orgName).isEqualTo("기존지점")
            assertThat(employee.costCenterCode).isEqualTo("0000")
            assertThat(employee.jikchak).isNull()
            // 발령일 도래 시 배치가 반영할 대상을 확정해 둔다 (SF PostponedAppointment__c 대응).
            assertThat(employee.postponedAppointment).isSameAs(appointment)
        }

        @Test
        @DisplayName("예약 발령 - 전문행사조는 건드리지 않음 (SF cls:179-202 유예 경로에 PPT 로직 없음)")
        fun reservedAppointmentKeepsPPT() {
            val employee = createEmployee(
                role = AppAuthority.WOMAN,
                professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE
            )
            every { employeeRepository.findByEmployeeCode("100234") } returns Optional.of(employee)

            val appointment = createAppointment(
                afterOrgCode = "1111", afterOrgName = "신규지점",
                jikchak = "D0053", jobCode = "A049",
                appointDate = LocalDate.of(2026, 3, 23), ordDetailNode = "전보"
            )

            updater.updateUserProfiles(listOf(appointment), today)

            assertThat(employee.crmWorkStartDate).isEqualTo(LocalDate.of(2026, 3, 23))
            // 즉시 반영이었다면 초기화 대상 조건(A049 + 비D0052 + 전보)이지만, 유예 시점에는 유지된다.
            assertThat(employee.professionalPromotionTeam).isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
        }
    }

    @Nested
    @DisplayName("User cost_center_code derived 캐시 동기화")
    inner class UserCostCenterCodeCacheTests {

        @BeforeEach
        fun setUp() {
            setupSystemCodeMaster()
        }

        @Test
        @DisplayName("발령 즉시 반영 시 매칭 User 의 cost_center_code 가 Employee 값으로 갱신")
        fun syncCostCenterCode() {
            val employee = createEmployee(costCenterCode = "0000")
            val user = User(
                username = "u@otoki.local",
                employeeCode = "100234",
                password = "x",
                costCenterCode = "0000",
            )
            every { employeeRepository.findByEmployeeCode("100234") } returns Optional.of(employee)
            every { userRepository.findByEmployeeCode("100234") } returns user

            val appointment = createAppointment(
                afterOrgCode = "1111", afterOrgName = "신규지점",
                jikchak = "D0053", jobCode = "A055",
                appointDate = LocalDate.of(2026, 3, 22), ordDetailNode = "전보"
            )

            updater.updateUserProfiles(listOf(appointment), today)

            assertThat(employee.costCenterCode).isEqualTo("1111")
            assertThat(user.costCenterCode).isEqualTo("1111")
        }

        @Test
        @DisplayName("매칭 User 없는 사원은 silent skip — 예외 없이 Employee 만 갱신")
        fun skipsWhenUserMissing() {
            val employee = createEmployee(costCenterCode = "0000")
            every { employeeRepository.findByEmployeeCode("100234") } returns Optional.of(employee)
            every { userRepository.findByEmployeeCode("100234") } returns null

            val appointment = createAppointment(
                afterOrgCode = "1111", afterOrgName = "신규지점",
                jikchak = "D0053", jobCode = "A055",
                appointDate = LocalDate.of(2026, 3, 22), ordDetailNode = "전보"
            )

            updater.updateUserProfiles(listOf(appointment), today)

            assertThat(employee.costCenterCode).isEqualTo("1111")
        }
    }

    @Nested
    @DisplayName("AppAuthority 결정 (jobCode 기반)")
    inner class JobCodeAuthorityTests {

        @Test
        @DisplayName("판촉직 조장 - A049 + D0052 -> 조장")
        fun promotionLeader() {
            val employee = createEmployee()
            updater.applyJobCodeAuthority(employee, "A049", "D0052")
            assertThat(employee.role).isEqualTo(AppAuthority.LEADER)
            assertThat(employee.appLoginActive).isTrue()
        }

        @Test
        @DisplayName("레이디직 여사원 - A053 + D0053 -> 여사원")
        fun ladyWorker() {
            val employee = createEmployee()
            updater.applyJobCodeAuthority(employee, "A053", "D0053")
            assertThat(employee.role).isEqualTo(AppAuthority.WOMAN)
            assertThat(employee.appLoginActive).isTrue()
        }

        @Test
        @DisplayName("일반직 - B001 -> 변경 없음")
        fun generalNoChange() {
            val employee = createEmployee(role = null, appLoginActive = false)
            updater.applyJobCodeAuthority(employee, "B001", "D0053")
            assertThat(employee.role).isEqualTo(null)
            assertThat(employee.appLoginActive).isFalse()
        }

        @Test
        @DisplayName("jobCode null -> 변경 없음")
        fun nullJobCode() {
            val employee = createEmployee(role = null)
            updater.applyJobCodeAuthority(employee, null, "D0052")
            assertThat(employee.role).isEqualTo(null)
        }
    }

    @Nested
    @DisplayName("ProfessionalPromotionTeam 초기화")
    inner class PPTResetTests {

        @Test
        @DisplayName("여사원 분기 발령(A049 + 비D0052) + 전보 -> 미배정(null)으로 초기화")
        fun resetToGeneral() {
            val employee = createEmployee(role = AppAuthority.WOMAN, professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)
            updater.applyProfessionalPromotionTeamReset(employee, "A049", "D0053", "전보")
            assertThat(employee.professionalPromotionTeam).isNull()
        }

        @Test
        @DisplayName("여사원 분기 발령 + 승진 -> 기존 값 유지")
        fun keepOnPromotion() {
            val employee = createEmployee(role = AppAuthority.WOMAN, professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)
            updater.applyProfessionalPromotionTeamReset(employee, "A049", "D0053", "승진")
            assertThat(employee.professionalPromotionTeam).isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
        }

        @Test
        @DisplayName("조장 발령(D0052) -> 초기화 안 함")
        fun leaderNoReset() {
            val employee = createEmployee(role = AppAuthority.LEADER, professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)
            updater.applyProfessionalPromotionTeamReset(employee, "A049", "D0052", "전보")
            assertThat(employee.professionalPromotionTeam).isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
        }

        @Test
        @DisplayName("비여사원 직무코드(A034) 발령 + 기존 여사원 -> 유지 (SF cls:143-151 — 발령 분기 기준 판정)")
        fun nonPromotionJobCodeKeepsPPT() {
            // role 기준으로 판정하던 회귀 방지 — 영업직 전환 발령이 전문행사조를 지우면 SF 와 어긋난다.
            val employee = createEmployee(role = AppAuthority.WOMAN, professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)
            updater.applyProfessionalPromotionTeamReset(employee, "A034", "D0053", "전보")
            assertThat(employee.professionalPromotionTeam).isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
        }

        @Test
        @DisplayName("jobCode null -> 유지")
        fun nullJobCodeKeepsPPT() {
            val employee = createEmployee(role = AppAuthority.WOMAN, professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE)
            updater.applyProfessionalPromotionTeamReset(employee, null, "D0053", "전보")
            assertThat(employee.professionalPromotionTeam).isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
        }
    }

    @Nested
    @DisplayName("배치 전용 반영 (applyPostponedAppointment — SF PostponedAppointmentBatch.cls:100-143)")
    inner class PostponedApplyTests {

        private val codeMap = mapOf(
            "H20020:D0052" to "판매조장",
            "H10060:A049" to "판촉직",
            "H10060:A053" to "레이디직"
        )

        @Test
        @DisplayName("발령일 = 레코드값, 조직명 prefix 없음, 예약 소진")
        fun batchSemantics() {
            val reserved = createAppointment(
                afterOrgCode = "3228", afterOrgName = "강남지점",
                jikchak = "D0052", jobCode = "A049",
                appointDate = LocalDate.of(2026, 4, 1), ordDetailNode = "조직개편"
            )
            val employee = createEmployee().apply {
                crmWorkStartDate = LocalDate.of(2026, 4, 1)
                postponedAppointment = reserved
            }

            updater.applyPostponedAppointment(employee, reserved, codeMap)

            assertThat(employee.appointmentDate).isEqualTo(LocalDate.of(2026, 4, 1))
            // 트리거(cls:160-167)와 달리 배치(cls:104)는 유통총괄 prefix 를 붙이지 않는다.
            assertThat(employee.orgName).isEqualTo("강남지점")
            assertThat(employee.costCenterCode).isEqualTo("3228")
            assertThat(employee.jikchak).isEqualTo("판매조장")
            assertThat(employee.jobCode).isEqualTo("판촉직")
            assertThat(employee.role).isEqualTo(AppAuthority.LEADER)
            assertThat(employee.appLoginActive).isTrue()
            // 유예된 발령정보 초기화 (cls:140-141)
            assertThat(employee.crmWorkStartDate).isNull()
            assertThat(employee.postponedAppointment).isNull()
        }

        @Test
        @DisplayName("A053(레이디직) - 배치는 AppAuthority 미갱신 (cls:116 — 트리거와 비대칭)")
        fun a053ExcludedFromAuthority() {
            val reserved = createAppointment(
                afterOrgCode = "1111", afterOrgName = "지점",
                jikchak = "D0053", jobCode = "A053",
                appointDate = LocalDate.of(2026, 4, 1)
            )
            val employee = createEmployee(role = null, appLoginActive = false).apply {
                postponedAppointment = reserved
            }

            updater.applyPostponedAppointment(employee, reserved, codeMap)

            assertThat(employee.jobCode).isEqualTo("레이디직")
            assertThat(employee.role).isNull()
            assertThat(employee.appLoginActive).isFalse()
        }

        @Test
        @DisplayName("전문행사조 미초기화 - 배치에는 PPT 로직 없음")
        fun pptUntouched() {
            val reserved = createAppointment(
                afterOrgCode = "1111", afterOrgName = "지점",
                jikchak = "D0053", jobCode = "A049",
                appointDate = LocalDate.of(2026, 4, 1), ordDetailNode = "전보"
            )
            val employee = createEmployee(
                role = AppAuthority.WOMAN,
                professionalPromotionTeam = ProfessionalPromotionTeamType.RAMEN_SALE
            ).apply { postponedAppointment = reserved }

            // 트리거 즉시 경로였다면 초기화 조건(A049 + 비D0052 + 전보)이지만, 배치는 건드리지 않는다.
            updater.applyPostponedAppointment(employee, reserved, codeMap)

            assertThat(employee.professionalPromotionTeam).isEqualTo(ProfessionalPromotionTeamType.RAMEN_SALE)
        }
    }

    @Nested
    @DisplayName("스킵 케이스")
    inner class SkipTests {

        @BeforeEach
        fun setUp() {
            setupSystemCodeMaster()
        }

        @Test
        @DisplayName("empCodeExist=false -> skip")
        fun skipWhenEmpCodeExistFalse() {
            val appointment = createAppointment(empCodeExist = false, afterOrgCode = "1111")
            updater.updateUserProfiles(listOf(appointment), today)
        }

        @Test
        @DisplayName("afterOrgCode=null -> skip 아님, costCenterCode=null 로 그대로 반영 (SF cls:101-204 게이트 없음)")
        fun afterOrgCodeNullStillApplies() {
            val employee = createEmployee(costCenterCode = "0000", orgName = "기존지점")
            every { employeeRepository.findByEmployeeCode("100234") } returns Optional.of(employee)

            val appointment = createAppointment(
                afterOrgCode = null, afterOrgName = "신규지점",
                jikchak = "D0053", jobCode = "A049",
                appointDate = LocalDate.of(2026, 3, 22)
            )

            updater.updateUserProfiles(listOf(appointment), today)

            // SF: OrgCode__c null → CostCenterCode__c = null 저장 + 나머지 인사필드는 정상 반영.
            assertThat(employee.costCenterCode).isNull()
            assertThat(employee.orgName).isEqualTo("신규지점")
            assertThat(employee.jikchak).isEqualTo("사원")
            assertThat(employee.jobCode).isEqualTo("판촉직")
        }

    }

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "100234",
        role: String? = null,
        appLoginActive: Boolean? = null,
        orgName: String? = null,
        costCenterCode: String? = null,
        professionalPromotionTeam: ProfessionalPromotionTeamType? = null
    ): Employee = Employee(
        id = id,
        employeeCode = employeeCode,
        name = "테스트사원",
        role = role,
        appLoginActive = appLoginActive,
        orgName = orgName,
        costCenterCode = costCenterCode,
        professionalPromotionTeam = professionalPromotionTeam
    )

    private fun createAppointment(
        employeeCode: String = "100234",
        empCodeExist: Boolean = true,
        afterOrgCode: String? = null,
        afterOrgName: String? = null,
        jikchak: String? = null,
        jikwee: String? = null,
        jikgub: String? = null,
        workType: String? = null,
        jobCode: String? = null,
        workArea: String? = null,
        jikjong: String? = null,
        appointDate: LocalDate = LocalDate.of(2026, 3, 22),
        ordDetailNode: String? = null
    ): Appointment = Appointment(
        employeeCode = employeeCode,
        empCodeExist = empCodeExist,
        afterOrgCode = afterOrgCode,
        afterOrgName = afterOrgName,
        jikchak = jikchak,
        jikwee = jikwee,
        jikgub = jikgub,
        workType = workType,
        jobCode = jobCode,
        workArea = workArea,
        jikjong = jikjong,
        appointDate = appointDate,
        ordDetailNode = ordDetailNode
    )

    private fun createSystemCodeMaster(
        groupCode: String,
        detailCode: String,
        detailCodeName: String
    ): SystemCodeMaster = SystemCodeMaster(
        companyCode = "1000",
        groupCode = groupCode,
        detailCode = detailCode,
        detailCodeName = detailCodeName,
        externalKey = "1000_${groupCode}_${detailCode}"
    )
}
