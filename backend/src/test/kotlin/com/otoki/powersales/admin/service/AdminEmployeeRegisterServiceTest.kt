package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.AdminEmployeeRegisterRequest
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.admin.exception.AdminPasswordPolicyViolationException
import com.otoki.powersales.admin.exception.EmployeeCodeDuplicatedException
import com.otoki.powersales.admin.exception.InvalidEmployeeCodeFormatException
import com.otoki.powersales.admin.exception.PasswordConfirmMismatchException
import com.otoki.powersales.admin.scope.AdminEmployeeHolder
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.enums.EmployeeOrigin
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminEmployeeRegisterService 테스트")
class AdminEmployeeRegisterServiceTest {

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var adminEmployeeHolder: AdminEmployeeHolder

    @InjectMocks
    private lateinit var service: AdminEmployeeRegisterService

    private val systemAdminActor = Employee(
        employeeCode = "ADMIN-OWNER",
        name = "기존관리자"
    ).apply {
        role = UserRole.SYSTEM_ADMIN
    }

    private val womanActor = Employee(
        employeeCode = "EMP-001",
        name = "여사원"
    ).apply {
        role = UserRole.WOMAN
    }

    private fun request(
        employeeCode: String = "ADMIN-001",
        name: String = "홍길동",
        password: String = "Admin@2026!",
        passwordConfirm: String = "Admin@2026!",
        workEmail: String? = "admin01@otoki.co.kr",
        workPhone: String? = "02-1234-5678",
        orgName: String? = "본사 IT팀",
        costCenterCode: String? = "IT001"
    ) = AdminEmployeeRegisterRequest(
        employeeCode = employeeCode,
        name = name,
        password = password,
        passwordConfirm = passwordConfirm,
        workEmail = workEmail,
        workPhone = workPhone,
        orgName = orgName,
        costCenterCode = costCenterCode
    )

    @BeforeEach
    fun setUp() {
        whenever(adminEmployeeHolder.require()).thenReturn(systemAdminActor)
    }

    @Nested
    @DisplayName("register - 정상 등록")
    inner class HappyPath {

        @Test
        @DisplayName("정상 등록 - SYSTEM_ADMIN 호출자 -> Employee 저장, role/origin/appLoginActive 고정")
        fun success() {
            whenever(employeeRepository.existsByEmployeeCode("ADMIN-001")).thenReturn(false)
            whenever(passwordEncoder.encode("Admin@2026!")).thenReturn("\$2a\$10\$encoded")
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.getArgument<Employee>(0) }

            val response = service.register(request())

            val captor = argumentCaptor<Employee>()
            verify(employeeRepository).save(captor.capture())
            val saved = captor.firstValue
            assertThat(saved.employeeCode).isEqualTo("ADMIN-001")
            assertThat(saved.role).isEqualTo(UserRole.SYSTEM_ADMIN)
            assertThat(saved.origin).isEqualTo(EmployeeOrigin.MANUAL)
            assertThat(saved.appLoginActive).isFalse
            assertThat(saved.passwordChangeRequired).isTrue
            assertThat(saved.password).isEqualTo("\$2a\$10\$encoded")

            assertThat(response.role).isEqualTo(UserRole.SYSTEM_ADMIN)
            assertThat(response.origin).isEqualTo(EmployeeOrigin.MANUAL)
            assertThat(response.appLoginActive).isFalse
            assertThat(response.passwordChangeRequired).isTrue
        }

        @Test
        @DisplayName("BCrypt 해시 - 평문 password 가 그대로 저장되지 않고 인코더 결과로 저장")
        fun encodesPassword() {
            whenever(employeeRepository.existsByEmployeeCode(any())).thenReturn(false)
            whenever(passwordEncoder.encode("Admin@2026!")).thenReturn("HASHED")
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.getArgument<Employee>(0) }

            service.register(request())

            val captor = argumentCaptor<Employee>()
            verify(employeeRepository).save(captor.capture())
            assertThat(captor.firstValue.password)
                .isEqualTo("HASHED")
                .isNotEqualTo("Admin@2026!")
        }
    }

    @Nested
    @DisplayName("register - 검증 실패")
    inner class ValidationErrors {

        @Test
        @DisplayName("권한 없음 - WOMAN role 호출자 -> AdminForbiddenException")
        fun forbiddenWhenNotSystemAdmin() {
            whenever(adminEmployeeHolder.require()).thenReturn(womanActor)

            assertThatThrownBy { service.register(request()) }
                .isInstanceOf(AdminForbiddenException::class.java)

            verify(employeeRepository, never()).save(any<Employee>())
        }

        @Test
        @DisplayName("사번 prefix 위반 - EMP-001 -> InvalidEmployeeCodeFormatException")
        fun invalidPrefix() {
            assertThatThrownBy { service.register(request(employeeCode = "EMP-001")) }
                .isInstanceOf(InvalidEmployeeCodeFormatException::class.java)
        }

        @Test
        @DisplayName("사번 형식 위반 - 한글 포함 -> InvalidEmployeeCodeFormatException")
        fun invalidCharacters() {
            assertThatThrownBy { service.register(request(employeeCode = "ADMIN-홍길동")) }
                .isInstanceOf(InvalidEmployeeCodeFormatException::class.java)
        }

        @Test
        @DisplayName("비밀번호 불일치 - password ≠ passwordConfirm -> PasswordConfirmMismatchException")
        fun passwordConfirmMismatch() {
            assertThatThrownBy {
                service.register(request(password = "Admin@2026!", passwordConfirm = "Other@2026!"))
            }.isInstanceOf(PasswordConfirmMismatchException::class.java)
        }

        @Test
        @DisplayName("비밀번호 정책 - 7자 -> AdminPasswordPolicyViolationException")
        fun passwordTooShort() {
            assertThatThrownBy {
                service.register(request(password = "Ab1!def", passwordConfirm = "Ab1!def"))
            }.isInstanceOf(AdminPasswordPolicyViolationException::class.java)
        }

        @Test
        @DisplayName("비밀번호 정책 - 영문만 8자 -> AdminPasswordPolicyViolationException")
        fun passwordSingleCategory() {
            assertThatThrownBy {
                service.register(request(password = "abcdefgh", passwordConfirm = "abcdefgh"))
            }.isInstanceOf(AdminPasswordPolicyViolationException::class.java)
        }

        @Test
        @DisplayName("비밀번호 정책 - 동일 문자 4회 반복 -> AdminPasswordPolicyViolationException")
        fun passwordConsecutiveSameChars() {
            assertThatThrownBy {
                service.register(request(password = "Abcd1111", passwordConfirm = "Abcd1111"))
            }.isInstanceOf(AdminPasswordPolicyViolationException::class.java)
        }

        @Test
        @DisplayName("사번 중복 - existsByEmployeeCode true -> EmployeeCodeDuplicatedException")
        fun duplicatedEmployeeCode() {
            whenever(employeeRepository.existsByEmployeeCode("ADMIN-001")).thenReturn(true)

            assertThatThrownBy { service.register(request()) }
                .isInstanceOf(EmployeeCodeDuplicatedException::class.java)

            verify(employeeRepository, never()).save(any<Employee>())
        }

        @Test
        @DisplayName("DB UNIQUE 충돌 - DataIntegrityViolationException -> EmployeeCodeDuplicatedException")
        fun raceConditionUniqueViolation() {
            whenever(employeeRepository.existsByEmployeeCode("ADMIN-001")).thenReturn(false)
            whenever(passwordEncoder.encode(any())).thenReturn("HASHED")
            whenever(employeeRepository.save(any<Employee>()))
                .thenThrow(DataIntegrityViolationException("duplicate key"))

            assertThatThrownBy { service.register(request()) }
                .isInstanceOf(EmployeeCodeDuplicatedException::class.java)
        }
    }
}
