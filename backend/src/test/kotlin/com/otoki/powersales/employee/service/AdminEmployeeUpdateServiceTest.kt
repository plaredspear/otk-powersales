package com.otoki.powersales.employee.service

import com.otoki.powersales.admin.exception.EmployeeNotFoundException
import com.otoki.powersales.admin.exception.SapOriginEmployeeNotEditableException
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.employee.dto.request.AdminEmployeeUpdateRequest
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.enums.EmployeeOrigin
import com.otoki.powersales.employee.repository.EmployeeRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AdminEmployeeUpdateService 테스트 (UC-07)")
class AdminEmployeeUpdateServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()

    private val service = AdminEmployeeUpdateService(
        employeeRepository,
    )

    @Test
    @DisplayName("origin=MANUAL 사원 -> 정보 수정 성공 + 응답에 새 값 반영")
    fun update_manualOrigin_success() {
        val existing = Employee(
            id = 10L,
            employeeCode = "100100",
            name = "수정전",
        ).apply {
            origin = EmployeeOrigin.MANUAL
            jikchak = "기존직책"
            role = UserRole.WOMAN
        }
        every { employeeRepository.findWithEmployeeInfoById(10L) } returns existing
        every { employeeRepository.save(any<Employee>()) } answers { firstArg() }

        val request = AdminEmployeeUpdateRequest(
            jikchak = "새직책",
            role = UserRole.LEADER,
            orgName = "신규조직",
        )

        val response = service.update(10L, request)

        assertThat(response.jikchak).isEqualTo("새직책")
        assertThat(response.role).isEqualTo("LEADER")
        assertThat(response.orgName).isEqualTo("신규조직")
    }

    @Test
    @DisplayName("origin=SAP 사원 -> SapOriginEmployeeNotEditableException")
    fun update_sapOrigin_blocked() {
        val sapEmployee = Employee(id = 11L, employeeCode = "100200", name = "SAP사원")
            .apply { origin = EmployeeOrigin.SAP }
        every { employeeRepository.findWithEmployeeInfoById(11L) } returns sapEmployee

        assertThatThrownBy {
            service.update(11L, AdminEmployeeUpdateRequest(jikchak = "변경시도"))
        }.isInstanceOf(SapOriginEmployeeNotEditableException::class.java)
            .hasMessageContaining("100200")
    }

    @Test
    @DisplayName("존재하지 않는 사원 -> EmployeeNotFoundException")
    fun update_notFound() {
        every { employeeRepository.findWithEmployeeInfoById(999L) } returns null

        assertThatThrownBy {
            service.update(999L, AdminEmployeeUpdateRequest())
        }.isInstanceOf(EmployeeNotFoundException::class.java)
    }

    @Test
    @DisplayName("Trigger 부수 효과 - 잠금 ON -> 앱 로그인 자동 OFF")
    fun update_lockingFlag_disablesAppLogin() {
        val existing = Employee(id = 12L, employeeCode = "100300", name = "잠금테스트")
            .apply {
                origin = EmployeeOrigin.MANUAL
                appLoginActive = true
                lockingFlag = false
            }
        every { employeeRepository.findWithEmployeeInfoById(12L) } returns existing
        every { employeeRepository.save(any<Employee>()) } answers { firstArg() }

        val response = service.update(
            12L,
            AdminEmployeeUpdateRequest(lockingFlag = true, appLoginActive = true)
        )

        assertThat(response.lockingFlag).isTrue()
        assertThat(response.appLoginActive).isFalse() // Trigger 자동 비활성화
    }
}
