package com.otoki.powersales.platform.push.service

import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeInfoRepository
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("FcmTokenService 테스트")
class FcmTokenServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val employeeInfoRepository: EmployeeInfoRepository = mockk(relaxed = true)
    private val pushBadgeService: PushBadgeService = mockk(relaxed = true)
    private val service = FcmTokenService(employeeRepository, employeeInfoRepository, pushBadgeService)

    @Test
    @DisplayName("register -> 인증 사용자의 fcmToken 을 갱신")
    fun register_updatesToken() {
        val employee = Employee(id = 1L, employeeCode = "100123", name = "테스트")
        every { employeeRepository.findWithEmployeeInfoById(1L) } returns employee

        service.register(1L, "fcm-token-abc")

        assertThat(employee.fcmToken).isEqualTo("fcm-token-abc")
    }

    @Test
    @DisplayName("register -> 같은 토큰을 보유한 다른 사원의 토큰을 먼저 해제 (한 단말 계정 전환)")
    fun register_releasesTokenFromOtherEmployees() {
        val employee = Employee(id = 2L, employeeCode = "100456", name = "새사용자")
        every { employeeRepository.findWithEmployeeInfoById(2L) } returns employee

        service.register(2L, "shared-device-token")

        // 이전 사원 해제가 본인 entity 로딩보다 먼저 실행돼야 한다 — 벌크 UPDATE 의
        // clearAutomatically 가 영속성 컨텍스트를 비우므로 순서가 뒤바뀌면 본인 변경이 유실된다.
        verifyOrder {
            employeeInfoRepository.releaseFcmTokenFromOtherEmployees("shared-device-token", 2L)
            employeeRepository.findWithEmployeeInfoById(2L)
        }
        assertThat(employee.fcmToken).isEqualTo("shared-device-token")
    }

    @Test
    @DisplayName("register -> 같은 사원이 다른 단말로 로그인하면 토큰이 새 값으로 교체")
    fun register_replacesTokenOnDeviceChange() {
        val employee = Employee(id = 1L, employeeCode = "100123", name = "테스트", fcmToken = "old-device-token")
        every { employeeRepository.findWithEmployeeInfoById(1L) } returns employee

        service.register(1L, "new-device-token")

        // 한 계정 한 단말 — 이전 단말 토큰은 남지 않는다.
        assertThat(employee.fcmToken).isEqualTo("new-device-token")
    }

    @Test
    @DisplayName("unregister -> fcmToken 을 null 로 해제하고 배지 카운터도 리셋")
    fun unregister_clearsToken() {
        val employee = Employee(id = 1L, employeeCode = "100123", name = "테스트", fcmToken = "old-token")
        every { employeeRepository.findWithEmployeeInfoById(1L) } returns employee

        service.unregister(1L)

        assertThat(employee.fcmToken).isNull()
        // 같은 단말에 다음 사용자가 로그인해도 이전 사용자의 미확인 건수가 이어지지 않아야 한다.
        verify(exactly = 1) { pushBadgeService.clear(1L) }
    }

    @Test
    @DisplayName("register -> 사용자 미존재 시 EmployeeNotFoundException")
    fun register_notFound() {
        every { employeeRepository.findWithEmployeeInfoById(99L) } returns null

        assertThatThrownBy { service.register(99L, "token") }
            .isInstanceOf(EmployeeNotFoundException::class.java)
    }
}
