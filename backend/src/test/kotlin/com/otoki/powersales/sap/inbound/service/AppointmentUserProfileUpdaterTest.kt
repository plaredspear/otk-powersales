package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.schedule.entity.Appointment
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.common.entity.SystemCodeMaster
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.organization.repository.OrganizationRepository
import com.otoki.powersales.common.repository.SystemCodeMasterRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("AppointmentUserProfileUpdater 테스트")
class AppointmentUserProfileUpdaterTest {

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var organizationRepository: OrganizationRepository

    @Mock
    private lateinit var systemCodeMasterRepository: SystemCodeMasterRepository

    @InjectMocks
    private lateinit var updater: AppointmentUserProfileUpdater

    private val today = LocalDate.of(2026, 3, 22)

    private fun setupSystemCodeMaster() {
        whenever(systemCodeMasterRepository.findByGroupCodeIn(
            listOf("H20020", "H20030", "H20010", "H10050", "H10060")
        )).thenReturn(listOf(
            createSystemCodeMaster("H20020", "D0052", "조장"),
            createSystemCodeMaster("H20020", "D0053", "사원"),
            createSystemCodeMaster("H20030", "W0010", "사원"),
            createSystemCodeMaster("H20010", "G0030", "3급"),
            createSystemCodeMaster("H10050", "T0010", "영업직"),
            createSystemCodeMaster("H10060", "A055", "OSC직"),
            createSystemCodeMaster("H10060", "A049", "판촉직"),
            createSystemCodeMaster("H10060", "A053", "레이디직"),
            createSystemCodeMaster("H10060", "B001", "일반직")
        ))
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
            whenever(employeeRepository.findByEmployeeCode("100234")).thenReturn(Optional.of(employee))

            val appointment = createAppointment(
                afterOrgCode = "1111", afterOrgName = "제1영업지점",
                jikchak = "D0052", jikwee = "W0010", jikgub = "G0030",
                workType = "T0010", jobCode = "A055",
                workArea = "서울", jikjong = "영업",
                appointDate = "20260322", ordDetailNode = "전보"
            )

            updater.updateUserProfiles(listOf(appointment), today)

            assertThat(employee.appAuthority).isEqualTo("조장")
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
            assertThat(employee.crmWorkStartDate).isNull()
        }

        @Test
        @DisplayName("여사원 - jobCode=A049, jikchak!=D0052, ordDetailNode=전보 -> 여사원, professionalPromotionTeam=일반")
        fun immediateWorker() {
            val employee = createEmployee()
            whenever(employeeRepository.findByEmployeeCode("100234")).thenReturn(Optional.of(employee))

            val appointment = createAppointment(
                afterOrgCode = "1111", afterOrgName = "제2영업지점",
                jikchak = "D0053", jobCode = "A049",
                appointDate = "20260322", ordDetailNode = "전보"
            )

            updater.updateUserProfiles(listOf(appointment), today)

            assertThat(employee.appAuthority).isEqualTo("여사원")
            assertThat(employee.appLoginActive).isTrue()
            assertThat(employee.professionalPromotionTeam).isEqualTo("일반")
        }

        @Test
        @DisplayName("일반직 - jobCode=B001 -> appAuthority/appLoginActive 변경 없음")
        fun immediateGeneral() {
            val employee = createEmployee(appAuthority = "영업사원", appLoginActive = false)
            whenever(employeeRepository.findByEmployeeCode("100234")).thenReturn(Optional.of(employee))

            val appointment = createAppointment(
                afterOrgCode = "1111", afterOrgName = "본사",
                jikchak = "D0053", jobCode = "B001",
                appointDate = "20260322"
            )

            updater.updateUserProfiles(listOf(appointment), today)

            assertThat(employee.appAuthority).isEqualTo("영업사원")
            assertThat(employee.appLoginActive).isFalse()
            assertThat(employee.costCenterCode).isEqualTo("1111")
            assertThat(employee.jikchak).isEqualTo("사원")
            assertThat(employee.jobCode).isEqualTo("일반직")
        }

        @Test
        @DisplayName("승진 발령 - ordDetailNode=승진 -> professionalPromotionTeam 기존 값 유지")
        fun promotionKeepsPPT() {
            val employee = createEmployee(professionalPromotionTeam = "특별반")
            whenever(employeeRepository.findByEmployeeCode("100234")).thenReturn(Optional.of(employee))

            val appointment = createAppointment(
                afterOrgCode = "1111", afterOrgName = "지점",
                jikchak = "D0053", jobCode = "A055",
                appointDate = "20260322", ordDetailNode = "승진"
            )

            updater.updateUserProfiles(listOf(appointment), today)

            assertThat(employee.appAuthority).isEqualTo("여사원")
            assertThat(employee.professionalPromotionTeam).isEqualTo("특별반")
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
        @DisplayName("코드 변환 실패 - 매칭 없음 -> 원본 코드 반환")
        fun codeResolutionFallback() {
            val codeMap = mapOf("H20020:D0052" to "조장")
            val result = updater.resolveCode(codeMap, "H20020", "UNKNOWN")
            assertThat(result).isEqualTo("UNKNOWN")
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
            whenever(employeeRepository.findByEmployeeCode("100234")).thenReturn(Optional.of(employee))

            val appointment = createAppointment(
                afterOrgCode = "1111", afterOrgName = "신규지점",
                jikchak = "D0052", jobCode = "A055",
                appointDate = "20260323"
            )

            updater.updateUserProfiles(listOf(appointment), today)

            assertThat(employee.crmWorkStartDate).isEqualTo(LocalDate.of(2026, 3, 23))
            assertThat(employee.appAuthority).isEqualTo("조장")
            assertThat(employee.appLoginActive).isTrue()
            assertThat(employee.orgName).isEqualTo("기존지점")
            assertThat(employee.costCenterCode).isEqualTo("0000")
            assertThat(employee.jikchak).isNull()
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
            assertThat(employee.appAuthority).isEqualTo("조장")
            assertThat(employee.appLoginActive).isTrue()
        }

        @Test
        @DisplayName("레이디직 여사원 - A053 + D0053 -> 여사원")
        fun ladyWorker() {
            val employee = createEmployee()
            updater.applyJobCodeAuthority(employee, "A053", "D0053")
            assertThat(employee.appAuthority).isEqualTo("여사원")
            assertThat(employee.appLoginActive).isTrue()
        }

        @Test
        @DisplayName("일반직 - B001 -> 변경 없음")
        fun generalNoChange() {
            val employee = createEmployee(appAuthority = "영업사원", appLoginActive = false)
            updater.applyJobCodeAuthority(employee, "B001", "D0053")
            assertThat(employee.appAuthority).isEqualTo("영업사원")
            assertThat(employee.appLoginActive).isFalse()
        }

        @Test
        @DisplayName("jobCode null -> 변경 없음")
        fun nullJobCode() {
            val employee = createEmployee(appAuthority = "영업사원")
            updater.applyJobCodeAuthority(employee, null, "D0052")
            assertThat(employee.appAuthority).isEqualTo("영업사원")
        }
    }

    @Nested
    @DisplayName("ProfessionalPromotionTeam 초기화")
    inner class PPTResetTests {

        @Test
        @DisplayName("여사원 + 전보 -> 일반으로 초기화")
        fun resetToGeneral() {
            val employee = createEmployee(appAuthority = "여사원", professionalPromotionTeam = "특별반")
            updater.applyProfessionalPromotionTeamReset(employee, "전보")
            assertThat(employee.professionalPromotionTeam).isEqualTo("일반")
        }

        @Test
        @DisplayName("여사원 + 승진 -> 기존 값 유지")
        fun keepOnPromotion() {
            val employee = createEmployee(appAuthority = "여사원", professionalPromotionTeam = "특별반")
            updater.applyProfessionalPromotionTeamReset(employee, "승진")
            assertThat(employee.professionalPromotionTeam).isEqualTo("특별반")
        }

        @Test
        @DisplayName("조장 -> 초기화 안 함")
        fun leaderNoReset() {
            val employee = createEmployee(appAuthority = "조장", professionalPromotionTeam = "특별반")
            updater.applyProfessionalPromotionTeamReset(employee, "전보")
            assertThat(employee.professionalPromotionTeam).isEqualTo("특별반")
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
        @DisplayName("afterOrgCode=null -> skip")
        fun skipWhenAfterOrgCodeNull() {
            val appointment = createAppointment(afterOrgCode = null)
            updater.updateUserProfiles(listOf(appointment), today)
        }

        @Test
        @DisplayName("appointDate 파싱 실패 -> skip")
        fun skipOnInvalidDate() {
            val employee = createEmployee()
            whenever(employeeRepository.findByEmployeeCode("100234")).thenReturn(Optional.of(employee))

            val appointment = createAppointment(
                afterOrgCode = "1111", appointDate = "invalid"
            )

            updater.updateUserProfiles(listOf(appointment), today)
            assertThat(employee.costCenterCode).isNull()
        }
    }

    @Nested
    @DisplayName("parseAppointDate")
    inner class ParseAppointDateTests {

        @Test
        @DisplayName("유효한 날짜 파싱")
        fun validDate() {
            val result = updater.parseAppointDate("20260322")
            assertThat(result).isEqualTo(LocalDate.of(2026, 3, 22))
        }

        @Test
        @DisplayName("null 입력 -> null")
        fun nullInput() {
            assertThat(updater.parseAppointDate(null)).isNull()
        }

        @Test
        @DisplayName("잘못된 형식 -> null")
        fun invalidFormat() {
            assertThat(updater.parseAppointDate("2026-03-22")).isNull()
        }
    }

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "100234",
        appAuthority: String? = null,
        appLoginActive: Boolean? = null,
        orgName: String? = null,
        costCenterCode: String? = null,
        professionalPromotionTeam: String? = null
    ): Employee = Employee(
        id = id,
        employeeCode = employeeCode,
        name = "테스트사원",
        appAuthority = appAuthority,
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
        appointDate: String = "20260322",
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
