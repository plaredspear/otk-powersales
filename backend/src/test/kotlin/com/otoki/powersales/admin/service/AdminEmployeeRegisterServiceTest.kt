package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.AdminEmployeeRegisterRequest
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.admin.exception.AdminPasswordPolicyViolationException
import com.otoki.powersales.admin.exception.EmployeeCodeDuplicatedException
import com.otoki.powersales.admin.exception.InvalidEmployeeCodeFormatException
import com.otoki.powersales.admin.exception.PasswordConfirmMismatchException
import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.enums.EmployeeOrigin
import com.otoki.powersales.employee.repository.EmployeeRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder

@DisplayName("AdminEmployeeRegisterService 테스트")
class AdminEmployeeRegisterServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()
    private val service = AdminEmployeeRegisterService(employeeRepository, passwordEncoder)

    private val systemAdminActor = principal(employeeId = 1L, employeeCode = "ADMIN-OWNER", role = UserRoleEnum.SYSTEM_ADMIN)
    private val womanActor = principal(employeeId = 2L, employeeCode = "EMP-001", role = UserRoleEnum.WOMAN)

    private fun principal(employeeId: Long, employeeCode: String, role: UserRoleEnum) = WebUserPrincipal(
        userId = employeeId * 10,
        usernameValue = employeeCode,
        employeeCode = employeeCode,
        employeeId = employeeId,
        role = role,
        costCenterCode = null,
        profileName = "9. Staff",
        isSalesSupport = false,
        passwordChangeRequired = false,
        permissions = emptySet(),
        encodedPassword = "",
        grantedAuthorities = emptyList(),
        active = true
    )

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

    @Nested
    @DisplayName("register - 정상 등록")
    inner class HappyPath {

        @Test
        @DisplayName("정상 등록 - SYSTEM_ADMIN 호출자 -> Employee 저장, role/origin/appLoginActive 고정")
        fun success() {
            every { employeeRepository.existsByEmployeeCode("ADMIN-001") } returns false
            every { passwordEncoder.encode("Admin@2026!") } returns "\$2a\$10\$encoded"
            val savedSlot = slot<Employee>()
            every { employeeRepository.save(capture(savedSlot)) } answers { firstArg() }

            val response = service.register(systemAdminActor, request())

            val saved = savedSlot.captured
            assertThat(saved.employeeCode).isEqualTo("ADMIN-001")
            assertThat(saved.role).isEqualTo(UserRoleEnum.SYSTEM_ADMIN)
            assertThat(saved.origin).isEqualTo(EmployeeOrigin.MANUAL)
            assertThat(saved.appLoginActive).isFalse
            assertThat(saved.passwordChangeRequired).isTrue
            assertThat(saved.password).isEqualTo("\$2a\$10\$encoded")

            assertThat(response.role).isEqualTo(UserRoleEnum.SYSTEM_ADMIN)
            assertThat(response.origin).isEqualTo(EmployeeOrigin.MANUAL)
            assertThat(response.appLoginActive).isFalse
            assertThat(response.passwordChangeRequired).isTrue
        }

        @Test
        @DisplayName("BCrypt 해시 - 평문 password 가 그대로 저장되지 않고 인코더 결과로 저장")
        fun encodesPassword() {
            every { employeeRepository.existsByEmployeeCode(any()) } returns false
            every { passwordEncoder.encode("Admin@2026!") } returns "HASHED"
            val savedSlot = slot<Employee>()
            every { employeeRepository.save(capture(savedSlot)) } answers { firstArg() }

            service.register(systemAdminActor, request())

            assertThat(savedSlot.captured.password)
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
            assertThatThrownBy { service.register(womanActor, request()) }
                .isInstanceOf(AdminForbiddenException::class.java)

            verify(exactly = 0) { employeeRepository.save(any<Employee>()) }
        }

        @Test
        @DisplayName("사번 prefix 위반 - EMP-001 -> InvalidEmployeeCodeFormatException")
        fun invalidPrefix() {
            assertThatThrownBy { service.register(systemAdminActor, request(employeeCode = "EMP-001")) }
                .isInstanceOf(InvalidEmployeeCodeFormatException::class.java)
        }

        @Test
        @DisplayName("사번 형식 위반 - 한글 포함 -> InvalidEmployeeCodeFormatException")
        fun invalidCharacters() {
            assertThatThrownBy { service.register(systemAdminActor, request(employeeCode = "ADMIN-홍길동")) }
                .isInstanceOf(InvalidEmployeeCodeFormatException::class.java)
        }

        @Test
        @DisplayName("비밀번호 불일치 - password ≠ passwordConfirm -> PasswordConfirmMismatchException")
        fun passwordConfirmMismatch() {
            assertThatThrownBy {
                service.register(systemAdminActor, request(password = "Admin@2026!", passwordConfirm = "Other@2026!"))
            }.isInstanceOf(PasswordConfirmMismatchException::class.java)
        }

        @Test
        @DisplayName("비밀번호 정책 - 7자 -> AdminPasswordPolicyViolationException")
        fun passwordTooShort() {
            assertThatThrownBy {
                service.register(systemAdminActor, request(password = "Ab1!def", passwordConfirm = "Ab1!def"))
            }.isInstanceOf(AdminPasswordPolicyViolationException::class.java)
        }

        @Test
        @DisplayName("비밀번호 정책 - 영문만 8자 -> AdminPasswordPolicyViolationException")
        fun passwordSingleCategory() {
            assertThatThrownBy {
                service.register(systemAdminActor, request(password = "abcdefgh", passwordConfirm = "abcdefgh"))
            }.isInstanceOf(AdminPasswordPolicyViolationException::class.java)
        }

        @Test
        @DisplayName("비밀번호 정책 - 동일 문자 4회 반복 -> AdminPasswordPolicyViolationException")
        fun passwordConsecutiveSameChars() {
            assertThatThrownBy {
                service.register(systemAdminActor, request(password = "Abcd1111", passwordConfirm = "Abcd1111"))
            }.isInstanceOf(AdminPasswordPolicyViolationException::class.java)
        }

        @Test
        @DisplayName("사번 중복 - existsByEmployeeCode true -> EmployeeCodeDuplicatedException")
        fun duplicatedEmployeeCode() {
            every { employeeRepository.existsByEmployeeCode("ADMIN-001") } returns true

            assertThatThrownBy { service.register(systemAdminActor, request()) }
                .isInstanceOf(EmployeeCodeDuplicatedException::class.java)

            verify(exactly = 0) { employeeRepository.save(any<Employee>()) }
        }

        @Test
        @DisplayName("DB UNIQUE 충돌 - DataIntegrityViolationException -> EmployeeCodeDuplicatedException")
        fun raceConditionUniqueViolation() {
            every { employeeRepository.existsByEmployeeCode("ADMIN-001") } returns false
            every { passwordEncoder.encode(any()) } returns "HASHED"
            every { employeeRepository.save(any<Employee>()) } throws DataIntegrityViolationException("duplicate key")

            assertThatThrownBy { service.register(systemAdminActor, request()) }
                .isInstanceOf(EmployeeCodeDuplicatedException::class.java)
        }
    }
}
