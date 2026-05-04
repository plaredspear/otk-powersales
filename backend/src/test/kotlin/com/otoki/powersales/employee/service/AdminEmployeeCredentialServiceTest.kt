package com.otoki.powersales.employee.service

import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.exception.EmployeeLoginInactiveException
import com.otoki.powersales.employee.exception.EmployeeNotFoundException
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminEmployeeCredentialService 테스트 (Spec #582 P1-B)")
class AdminEmployeeCredentialServiceTest {

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMocks
    private lateinit var service: AdminEmployeeCredentialService

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
            whenever(employeeRepository.findById(eq(12345L))).thenReturn(Optional.of(employee))

            val response = service.resetDevice(12345L)

            assertThat(employee.deviceUuid).isNull()
            assertThat(response.employeeId).isEqualTo(12345L)
            assertThat(response.employeeCode).isEqualTo("100123")
            assertThat(response.previousDeviceBound).isTrue
        }

        @Test
        @DisplayName("정상 - 이미 deviceUuid=null 사원 -> previousDeviceBound=false")
        fun success_notPreviouslyBound() {
            val employee = activeEmployee(deviceUuid = null)
            whenever(employeeRepository.findById(eq(12345L))).thenReturn(Optional.of(employee))

            val response = service.resetDevice(12345L)

            assertThat(response.previousDeviceBound).isFalse
            assertThat(employee.deviceUuid).isNull()
        }

        @Test
        @DisplayName("실패 - 미존재 employeeId -> EmployeeNotFoundException")
        fun fail_notFound() {
            whenever(employeeRepository.findById(eq(99999999L))).thenReturn(Optional.empty())

            assertThatThrownBy { service.resetDevice(99999999L) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("실패 - appLoginActive=false 사원 -> EmployeeLoginInactiveException")
        fun fail_loginInactive() {
            val employee = activeEmployee().apply { appLoginActive = false }
            whenever(employeeRepository.findById(eq(12345L))).thenReturn(Optional.of(employee))

            assertThatThrownBy { service.resetDevice(12345L) }
                .isInstanceOf(EmployeeLoginInactiveException::class.java)

            assertThat(employee.deviceUuid).isEqualTo("DEVICE-AAA")
        }

        @Test
        @DisplayName("실패 - appLoginActive=null 사원 -> EmployeeLoginInactiveException")
        fun fail_loginInactiveNull() {
            val employee = activeEmployee().apply { appLoginActive = null }
            whenever(employeeRepository.findById(eq(12345L))).thenReturn(Optional.of(employee))

            assertThatThrownBy { service.resetDevice(12345L) }
                .isInstanceOf(EmployeeLoginInactiveException::class.java)
        }
    }

    @Nested
    @DisplayName("resetPassword - 비밀번호 임시 리셋")
    inner class ResetPassword {

        @Test
        @DisplayName("정상 - 임시 비밀번호 BCrypt 해시 저장 + passwordChangeRequired=true")
        fun success() {
            val employee = activeEmployee()
            whenever(employeeRepository.findById(eq(12345L))).thenReturn(Optional.of(employee))
            whenever(passwordEncoder.encode(eq(AdminEmployeeCredentialService.TEMPORARY_PASSWORD)))
                .thenReturn("BCRYPT_HASHED_1234")

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
            whenever(employeeRepository.findById(eq(12345L))).thenReturn(Optional.of(employee))
            whenever(passwordEncoder.encode(eq("1234"))).thenReturn("BCRYPT_HASHED")

            val response = service.resetPassword(12345L)

            // ResetPasswordResponse 의 모든 필드를 toString 으로 직렬화해도 "1234" 평문이 노출되지 않음
            assertThat(response.toString()).doesNotContain("\"1234\"")
        }

        @Test
        @DisplayName("실패 - 미존재 employeeId -> EmployeeNotFoundException")
        fun fail_notFound() {
            whenever(employeeRepository.findById(eq(99999999L))).thenReturn(Optional.empty())

            assertThatThrownBy { service.resetPassword(99999999L) }
                .isInstanceOf(EmployeeNotFoundException::class.java)

            verify(passwordEncoder, never()).encode(org.mockito.kotlin.any<String>())
        }

        @Test
        @DisplayName("실패 - appLoginActive=false 사원 -> EmployeeLoginInactiveException, 비밀번호 미변경")
        fun fail_loginInactive() {
            val employee = activeEmployee().apply { appLoginActive = false }
            whenever(employeeRepository.findById(eq(12345L))).thenReturn(Optional.of(employee))

            assertThatThrownBy { service.resetPassword(12345L) }
                .isInstanceOf(EmployeeLoginInactiveException::class.java)

            assertThat(employee.password).isEqualTo("ENCODED_OLD")
            verify(passwordEncoder, never()).encode(org.mockito.kotlin.any<String>())
        }
    }
}
