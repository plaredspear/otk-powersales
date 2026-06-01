package com.otoki.powersales.push.service

import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("FcmTokenService 테스트")
class FcmTokenServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val service = FcmTokenService(employeeRepository)

    @Test
    @DisplayName("register -> 인증 사용자의 fcmToken 을 갱신")
    fun register_updatesToken() {
        val employee = Employee(id = 1L, employeeCode = "100123", name = "테스트")
        every { employeeRepository.findWithEmployeeInfoById(1L) } returns employee

        service.register(1L, "fcm-token-abc")

        assertThat(employee.fcmToken).isEqualTo("fcm-token-abc")
    }

    @Test
    @DisplayName("unregister -> fcmToken 을 null 로 해제")
    fun unregister_clearsToken() {
        val employee = Employee(id = 1L, employeeCode = "100123", name = "테스트", fcmToken = "old-token")
        every { employeeRepository.findWithEmployeeInfoById(1L) } returns employee

        service.unregister(1L)

        assertThat(employee.fcmToken).isNull()
    }

    @Test
    @DisplayName("register -> 사용자 미존재 시 EmployeeNotFoundException")
    fun register_notFound() {
        every { employeeRepository.findWithEmployeeInfoById(99L) } returns null

        assertThatThrownBy { service.register(99L, "token") }
            .isInstanceOf(EmployeeNotFoundException::class.java)
    }
}
