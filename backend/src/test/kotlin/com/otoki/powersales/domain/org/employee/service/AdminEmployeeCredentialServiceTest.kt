package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.exception.EmployeeLoginInactiveException
import com.otoki.powersales.domain.org.employee.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.employee.service.AdminEmployeeCredentialService
import com.otoki.powersales.platform.common.security.ActiveDeviceStore
import com.otoki.powersales.platform.common.security.JwtTokenProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

@DisplayName("AdminEmployeeCredentialService 테스트 (Spec #582 P1-B)")
class AdminEmployeeCredentialServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()
    private val activeDeviceStore: ActiveDeviceStore = mockk(relaxed = true)
    private val jwtTokenProvider: JwtTokenProvider = mockk(relaxed = true)

    private val service = AdminEmployeeCredentialService(
        employeeRepository,
        passwordEncoder,
        activeDeviceStore,
        jwtTokenProvider,
    )

    private fun activeEmployee(
        id: Long = 12345L,
        employeeCode: String = "100123",
        deviceUuid: String? = "DEVICE-AAA"
    ) = Employee(
        id = id,
        employeeCode = employeeCode,
        name = "홍길동",
        deviceUuid = deviceUuid,
        password = "ENCODED_OLD"
    ).apply {
        appLoginActive = true
    }

    @Nested
    @DisplayName("resetDevice - 단말 초기화")
    inner class ResetDevice {

        @Test
        @DisplayName("정상 - active 사원 -> deviceUuid=null, previousDeviceBound=true")
        fun success_previouslyBound() {
            val employee = activeEmployee(deviceUuid = "DEVICE-AAA")
            every { employeeRepository.findById(12345L) } returns Optional.of(employee)

            val response = service.resetDevice(12345L)

            assertThat(employee.deviceUuid).isNull()
            assertThat(response.employeeId).isEqualTo(12345L)
            assertThat(response.employeeCode).isEqualTo("100123")
            assertThat(response.previousDeviceBound).isTrue
            // 기존 기기 즉시 차단: 활성기기 캐시 제거 + refresh token 무효화
            verify(exactly = 1) { activeDeviceStore.clearActiveDevice(12345L) }
            verify(exactly = 1) { jwtTokenProvider.deleteRefreshTokenByUserId(12345L) }
        }

        @Test
        @DisplayName("정상 - 이미 deviceUuid=null 사원 -> previousDeviceBound=false")
        fun success_notPreviouslyBound() {
            val employee = activeEmployee(deviceUuid = null)
            every { employeeRepository.findById(12345L) } returns Optional.of(employee)

            val response = service.resetDevice(12345L)

            assertThat(response.previousDeviceBound).isFalse
            assertThat(employee.deviceUuid).isNull()
        }

        @Test
        @DisplayName("실패 - 미존재 employeeId -> EmployeeNotFoundException")
        fun fail_notFound() {
            every { employeeRepository.findById(99999999L) } returns Optional.empty()

            assertThatThrownBy { service.resetDevice(99999999L) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("실패 - appLoginActive=false 사원 -> EmployeeLoginInactiveException")
        fun fail_loginInactive() {
            val employee = activeEmployee().apply { appLoginActive = false }
            every { employeeRepository.findById(12345L) } returns Optional.of(employee)

            assertThatThrownBy { service.resetDevice(12345L) }
                .isInstanceOf(EmployeeLoginInactiveException::class.java)

            assertThat(employee.deviceUuid).isEqualTo("DEVICE-AAA")
        }

        @Test
        @DisplayName("실패 - appLoginActive=null 사원 -> EmployeeLoginInactiveException")
        fun fail_loginInactiveNull() {
            val employee = activeEmployee().apply { appLoginActive = null }
            every { employeeRepository.findById(12345L) } returns Optional.of(employee)

            assertThatThrownBy { service.resetDevice(12345L) }
                .isInstanceOf(EmployeeLoginInactiveException::class.java)
        }
    }

    @Nested
    @DisplayName("resetPassword - 비밀번호 임시 리셋")
    inner class ResetPassword {

        @Test
        @DisplayName("정상 - 사번 기반 임시 비밀번호 BCrypt 해시 저장 + passwordChangeRequired=true")
        fun success() {
            val employee = activeEmployee()
            every { employeeRepository.findById(12345L) } returns Optional.of(employee)
            every { passwordEncoder.encode("100123@pwrs") } returns "BCRYPT_HASHED_1234"

            val response = service.resetPassword(12345L)

            assertThat(employee.password).isEqualTo("BCRYPT_HASHED_1234")
            assertThat(employee.passwordChangeRequired).isTrue
            assertThat(response.temporaryPasswordIssued).isTrue
            assertThat(response.passwordChangeRequired).isTrue
            assertThat(response.employeeCode).isEqualTo("100123")
        }

        @Test
        @DisplayName("응답에 임시 비밀번호 평문 없음 - 보안 정책")
        fun responseDoesNotIncludePlaintextPassword() {
            val employee = activeEmployee()
            every { employeeRepository.findById(12345L) } returns Optional.of(employee)
            every { passwordEncoder.encode("100123@pwrs") } returns "BCRYPT_HASHED"

            val response = service.resetPassword(12345L)

            // 사번(employeeCode) 은 응답에 담기되, 조립된 평문 "100123@pwrs" 자체는 노출하지 않는다.
            assertThat(response.toString()).doesNotContain("100123@pwrs")
            assertThat(response.toString()).doesNotContain("BCRYPT_HASHED")
        }

        @Test
        @DisplayName("실패 - 미존재 employeeId -> EmployeeNotFoundException")
        fun fail_notFound() {
            every { employeeRepository.findById(99999999L) } returns Optional.empty()

            assertThatThrownBy { service.resetPassword(99999999L) }
                .isInstanceOf(EmployeeNotFoundException::class.java)

            verify(exactly = 0) { passwordEncoder.encode(any<String>()) }
        }

        @Test
        @DisplayName("실패 - appLoginActive=false 사원 -> EmployeeLoginInactiveException, 비밀번호 미변경")
        fun fail_loginInactive() {
            val employee = activeEmployee().apply { appLoginActive = false }
            every { employeeRepository.findById(12345L) } returns Optional.of(employee)

            assertThatThrownBy { service.resetPassword(12345L) }
                .isInstanceOf(EmployeeLoginInactiveException::class.java)

            assertThat(employee.password).isEqualTo("ENCODED_OLD")
            verify(exactly = 0) { passwordEncoder.encode(any<String>()) }
        }
    }
}
