package com.otoki.powersales.employee.service

import com.otoki.powersales.common.entity.SystemCodeMaster
import com.otoki.powersales.common.repository.SystemCodeMasterRepository
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.enums.EmployeeOrigin
import com.otoki.powersales.employee.enums.Gender
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.employee.service.dto.EmployeeUpsertCommand
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmployeeUpsertService 테스트")
class EmployeeUpsertServiceTest {

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var systemCodeMasterRepository: SystemCodeMasterRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var service: EmployeeUpsertService

    @BeforeEach
    fun setUp() {
        // PasswordEncoder.encode 호출 발생 (Employee 신규 시) — Lenient stub 으로 모든 nested 클래스 공통 적용.
        whenever(passwordEncoder.encode(any<CharSequence>())).thenAnswer { it.arguments[0].toString() + ":encoded" }
    }

    private fun command(
        employeeCode: String? = "100123",
        employeeName: String? = "홍길동",
        gender: String? = null,
        homePhone: String? = null,
        workPhone: String? = null,
        workEmail: String? = null,
        email: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        status: String? = null,
        birthdate: String? = null,
        orgCode: String? = null,
        lockingFlag: String? = null
    ): EmployeeUpsertCommand = EmployeeUpsertCommand(
        employeeCode = employeeCode,
        employeeName = employeeName,
        gender = gender,
        homePhone = homePhone,
        workPhone = workPhone,
        workEmail = workEmail,
        email = email,
        startDate = startDate,
        endDate = endDate,
        status = status,
        birthdate = birthdate,
        orgCode = orgCode,
        lockingFlag = lockingFlag
    )

    private fun statusCode(detailCode: String, detailCodeName: String): SystemCodeMaster =
        SystemCodeMaster(
            companyCode = "1000",
            groupCode = "H10010",
            detailCode = detailCode,
            detailCodeName = detailCodeName,
            externalKey = "1000_H10010_$detailCode"
        )

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 직원 1건 - INSERT, success_count=1, EmployeeInfo cascade 자동 생성")
        fun upsert_insertNew() {
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("100123"))).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            val result = service.upsert(listOf(command()))

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved.employeeCode).isEqualTo("100123")
            assertThat(saved.name).isEqualTo("홍길동")
            assertThat(saved.employeeInfo).isNotNull
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(0)
        }

        @Test
        @DisplayName("기존 직원 갱신 - 동일 PK 유지, name/homePhone 업데이트")
        fun upsert_updateExisting() {
            val existing = Employee(employeeCode = "100123", name = "기존이름")
            existing.homePhone = "old-phone"
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("100123"))).thenReturn(listOf(existing))
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(listOf(command(employeeName = "새이름", homePhone = "new-phone")))

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved).isSameAs(existing)
            assertThat(saved.name).isEqualTo("새이름")
            assertThat(saved.homePhone).isEqualTo("new-phone")
        }

        @Test
        @DisplayName("Status 변환 성공 - SystemCodeMaster 매칭 코드명 적용")
        fun upsert_statusResolved() {
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")))
                .thenReturn(listOf(statusCode("10", "재직")))

            service.upsert(listOf(command(status = "10")))

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().status).isEqualTo("재직")
        }

        @Test
        @DisplayName("Status 매칭 실패 - 원본 코드 그대로 저장")
        fun upsert_statusUnmapped() {
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010")))
                .thenReturn(listOf(statusCode("10", "재직")))

            service.upsert(listOf(command(status = "99")))

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().status).isEqualTo("99")
        }

        @Test
        @DisplayName("companyCode 1000 외 행은 status 매핑에서 제외")
        fun upsert_statusFiltersByCompanyCode() {
            val other = SystemCodeMaster(
                companyCode = "2000",
                groupCode = "H10010",
                detailCode = "10",
                detailCodeName = "다른회사재직",
                externalKey = "2000_H10010_10"
            )
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(listOf(other))

            service.upsert(listOf(command(status = "10")))

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().status).isEqualTo("10")
        }

        @Test
        @DisplayName("Gender 변환 - '1' → MALE, '2' → FEMALE, 그 외 → null")
        fun upsert_sexConversion() {
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(
                listOf(
                    command(employeeCode = "M01", gender = "1"),
                    command(employeeCode = "F01", gender = "2"),
                    command(employeeCode = "X01", gender = "9")
                )
            )

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            val byCode = captor.firstValue.associateBy { it.employeeCode }
            assertThat(byCode["M01"]?.gender).isEqualTo(Gender.MALE)
            assertThat(byCode["F01"]?.gender).isEqualTo(Gender.FEMALE)
            assertThat(byCode["X01"]?.gender).isNull()
        }

        @Test
        @DisplayName("StartDate / EndDate / Birthdate - YYYYMMDD 변환, 00000000 은 null")
        fun upsert_dateConversion() {
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(
                listOf(command(startDate = "20200401", endDate = "00000000", birthdate = "19850315"))
            )

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved.startDate).isEqualTo(LocalDate.of(2020, 4, 1))
            assertThat(saved.endDate).isNull()
            assertThat(saved.birthDate).isEqualTo("19850315")
        }

        @Test
        @DisplayName("LockingFlag - Y → false, N → true, null → true")
        fun upsert_lockingFlag() {
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(
                listOf(
                    command(employeeCode = "L01", lockingFlag = "Y"),
                    command(employeeCode = "L02", lockingFlag = "N"),
                    command(employeeCode = "L03", lockingFlag = null)
                )
            )

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            val byCode = captor.firstValue.associateBy { it.employeeCode }
            assertThat(byCode["L01"]?.appLoginActive).isFalse
            assertThat(byCode["L02"]?.appLoginActive).isTrue
            assertThat(byCode["L03"]?.appLoginActive).isTrue
        }

        @Test
        @DisplayName("WorkEmail / Email - 신규 컬럼 저장")
        fun upsert_emailColumns() {
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(listOf(command(workEmail = "work@otoki.com", email = "personal@otoki.com")))

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().workEmail).isEqualTo("work@otoki.com")
            assertThat(captor.firstValue.single().email).isEqualTo("personal@otoki.com")
        }

        @Test
        @DisplayName("OrgCode → costCenterCode 매핑")
        fun upsert_orgCodeMapping() {
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(listOf(command(orgCode = "11110")))

            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().costCenterCode).isEqualTo("11110")
        }
    }

    @Nested
    @DisplayName("upsert - Spec #579 origin=MANUAL 보호")
    inner class ManualOriginProtection {

        @Test
        @DisplayName("기존 origin=MANUAL - 갱신 스킵 + protectedManualCodes 누적, successCount 영향 없음")
        fun upsert_manualOriginProtected() {
            val manualEmp = Employee(employeeCode = "M0001", name = "수동등록", origin = EmployeeOrigin.MANUAL)
            val sapEmp = Employee(employeeCode = "100123", name = "SAP기존")
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("M0001", "100123")))
                .thenReturn(listOf(manualEmp, sapEmp))
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            val result = service.upsert(
                listOf(
                    command(employeeCode = "M0001", employeeName = "갱신요청"),
                    command(employeeCode = "100123", employeeName = "SAP갱신")
                )
            )

            assertThat(result.successCount).isEqualTo(1) // SAP 직원만 갱신
            assertThat(result.protectedManualCodes).containsExactly("M0001")
            // SAP 직원만 saveAll 에 포함
            val captor = argumentCaptor<List<Employee>>()
            verify(employeeRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().employeeCode).isEqualTo("100123")
        }
    }

    @Nested
    @DisplayName("upsert - Error Path")
    inner class UpsertError {

        @Test
        @DisplayName("EmployeeCode 누락 - failures 기록, identifier null")
        fun upsert_missingEmployeeCode() {
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            val result = service.upsert(listOf(command(employeeCode = null)))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isNull()
            assertThat(result.failures.single().reason).contains("EmployeeCode 필수")
            verify(employeeRepository, never()).saveAll(any<List<Employee>>())
        }

        @Test
        @DisplayName("EmployeeName 누락 - failures 기록, identifier 보존")
        fun upsert_missingEmployeeName() {
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            val result = service.upsert(listOf(command(employeeName = null)))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("100123")
            assertThat(result.failures.single().reason).contains("EmployeeName 필수")
        }

        @Test
        @DisplayName("StartDate 형식 오류 - failures 기록")
        fun upsert_invalidStartDate() {
            whenever(employeeRepository.findByEmployeeCodeIn(any<List<String>>())).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            val result = service.upsert(listOf(command(startDate = "2020/04/01")))

            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().reason).contains("StartDate 형식 오류")
        }

        @Test
        @DisplayName("일부 행 실패 - 성공 행은 적재, 실패 행은 failures 누적")
        fun upsert_partialFailure() {
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("100123", "100124"))).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            val result = service.upsert(
                listOf(
                    command(employeeCode = "100123", employeeName = "정상"),
                    command(employeeCode = "100124", employeeName = null)
                )
            )

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failureCount).isEqualTo(1)
            assertThat(result.failures.single().identifier).isEqualTo("100124")
        }
    }

    @Nested
    @DisplayName("Spec #758 - Employee 신규 생성 시 User 자동 생성")
    inner class UserAutoCreation {

        @Test
        @DisplayName("U1 신규 Employee 인입 - User 1건 자동 생성 (employee_number 매칭)")
        fun upsert_newEmployee_createsUser() {
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("100123"))).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            val result = service.upsert(
                listOf(command(employeeCode = "100123", employeeName = "홍길동", workEmail = "hong@otokims.co.kr", birthdate = "19900315"))
            )

            assertThat(result.successCount).isEqualTo(1)
            val captor = argumentCaptor<List<User>>()
            verify(userRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved.employeeNumber).isEqualTo("100123")
            assertThat(saved.username).isEqualTo("hong@otokims.co.kr")
            assertThat(saved.email).isEqualTo("hong@otokims.co.kr")
            assertThat(saved.name).isEqualTo("홍길동")
            assertThat(saved.passwordChangeRequired).isTrue
            // Q5 정책: 사번 + 생년월일 끝 4자리 (MMdd) — passwordEncoder.encode 가 ":encoded" 접미
            assertThat(saved.password).isEqualTo("1001230315:encoded")
        }

        @Test
        @DisplayName("U2 기존 Employee 갱신 - User 생성 안 함")
        fun upsert_existingEmployee_skipsUserCreation() {
            val existing = Employee(employeeCode = "100123", name = "기존")
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("100123"))).thenReturn(listOf(existing))
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(listOf(command(employeeCode = "100123", employeeName = "갱신")))

            verify(userRepository, never()).saveAll(any<List<User>>())
        }

        @Test
        @DisplayName("U3 workEmail / email 부재 - User 생성 skip (SF IF_REST_SAP_EmployeeMaster.cls:281 동등)")
        fun upsert_noEmail_skipsUserCreation() {
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("100123"))).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(listOf(command(employeeCode = "100123", employeeName = "홍길동")))

            verify(userRepository, never()).saveAll(any<List<User>>())
        }

        @Test
        @DisplayName("U4 birthdate 부재 - 임시 비밀번호 = 사번 + '0000'")
        fun upsert_noBirthdate_passwordFallback() {
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("100123"))).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(listOf(command(employeeCode = "100123", employeeName = "홍길동", workEmail = "hong@otoki.com", birthdate = null)))

            val captor = argumentCaptor<List<User>>()
            verify(userRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().password).isEqualTo("1001230000:encoded")
        }

        @Test
        @DisplayName("U5 신규 + 기존 혼합 - 신규 행만 User 생성")
        fun upsert_partialNew_createsOnlyForNew() {
            val existing = Employee(employeeCode = "100100", name = "기존")
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("100100", "100200")))
                .thenReturn(listOf(existing))
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(
                listOf(
                    command(employeeCode = "100100", employeeName = "기존갱신"),
                    command(employeeCode = "100200", employeeName = "신규", workEmail = "new@otoki.com")
                )
            )

            val captor = argumentCaptor<List<User>>()
            verify(userRepository).saveAll(captor.capture())
            assertThat(captor.firstValue).hasSize(1)
            assertThat(captor.firstValue.single().employeeNumber).isEqualTo("100200")
        }

        @Test
        @DisplayName("U6 LockingFlag Y - User.isActive = false (Employee.appLoginActive 동기)")
        fun upsert_lockingFlagY_userInactive() {
            whenever(employeeRepository.findByEmployeeCodeIn(listOf("100123"))).thenReturn(emptyList())
            whenever(systemCodeMasterRepository.findByGroupCodeIn(listOf("H10010"))).thenReturn(emptyList())

            service.upsert(listOf(command(employeeCode = "100123", employeeName = "홍길동", workEmail = "hong@otoki.com", lockingFlag = "Y")))

            val captor = argumentCaptor<List<User>>()
            verify(userRepository).saveAll(captor.capture())
            assertThat(captor.firstValue.single().isActive).isFalse
        }
    }
}
