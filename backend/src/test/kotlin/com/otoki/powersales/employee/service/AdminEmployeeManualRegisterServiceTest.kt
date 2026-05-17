package com.otoki.powersales.employee.service

import com.otoki.powersales.admin.exception.EmployeeCodeDuplicatedException
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.employee.dto.request.AdminEmployeeManualRegisterRequest
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.enums.EmployeeOrigin
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminEmployeeManualRegisterService 테스트 (UC-06)")
class AdminEmployeeManualRegisterServiceTest {

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @InjectMocks
    private lateinit var service: AdminEmployeeManualRegisterService

    @Test
    @DisplayName("신규 등록 성공 -> origin=MANUAL + appLoginActive=false + 전화번호 미러링")
    fun register_success() {
        whenever(employeeRepository.existsByEmployeeCode("100400")).thenReturn(false)
        whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.arguments[0] as Employee }

        val request = AdminEmployeeManualRegisterRequest(
            employeeCode = "100400",
            name = "신규여사원",
            role = UserRole.WOMAN,
            homePhone = "010-1111-2222",
            orgName = "테스트조직",
            jobCode = "판촉직",
        )

        val response = service.register(request)

        assertThat(response.employeeCode).isEqualTo("100400")
        assertThat(response.name).isEqualTo("신규여사원")
        assertThat(response.origin).isEqualTo(EmployeeOrigin.MANUAL.name)
        assertThat(response.appLoginActive).isFalse()
        // 전화번호 미러링: homePhone -> phone 자동 채움
        assertThat(response.phone).isEqualTo("010-1111-2222")
        assertThat(response.homePhone).isEqualTo("010-1111-2222")
        assertThat(response.role).isEqualTo("WOMAN")
    }

    @Test
    @DisplayName("중복 사번 -> EmployeeCodeDuplicatedException")
    fun register_duplicate() {
        whenever(employeeRepository.existsByEmployeeCode("100500")).thenReturn(true)

        assertThatThrownBy {
            service.register(
                AdminEmployeeManualRegisterRequest(
                    employeeCode = "100500",
                    name = "중복",
                )
            )
        }.isInstanceOf(EmployeeCodeDuplicatedException::class.java)
    }

    @Test
    @DisplayName("SYSTEM_ADMIN 역할 -> require 실패 (IllegalArgumentException)")
    fun register_systemAdminRoleRejected() {
        assertThatThrownBy {
            service.register(
                AdminEmployeeManualRegisterRequest(
                    employeeCode = "100600",
                    name = "잘못된관리자",
                    role = UserRole.SYSTEM_ADMIN,
                )
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("SYSTEM_ADMIN")
    }
}
