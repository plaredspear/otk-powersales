package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.admin.exception.EmployeeCodeDuplicatedException
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.org.employee.dto.request.AdminEmployeeManualRegisterRequest
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.enums.EmployeeOrigin
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.employee.service.AdminEmployeeManualRegisterService
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AdminEmployeeManualRegisterService 테스트 (UC-06)")
class AdminEmployeeManualRegisterServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()

    private val service = AdminEmployeeManualRegisterService(
        employeeRepository,
    )

    @Test
    @DisplayName("신규 등록 성공 -> origin=MANUAL + appLoginActive=false + 전화번호 미러링")
    fun register_success() {
        every { employeeRepository.existsByEmployeeCode("100400") } returns false
        every { employeeRepository.save(any<Employee>()) } answers { firstArg() }

        val request = AdminEmployeeManualRegisterRequest(
            employeeCode = "100400",
            name = "신규여사원",
            role = AppAuthority.WOMAN,
            homePhone = "010-1111-2222",
            orgName = "테스트조직",
            jobCode = "판촉직",
        )

        val response = service.register(request)

        assertThat(response.employeeCode).isEqualTo("100400")
        assertThat(response.name).isEqualTo("신규여사원")
        assertThat(response.origin).isEqualTo(EmployeeOrigin.MANUAL.name)
        assertThat(response.appLoginActive).isFalse()
        assertThat(response.phone).isEqualTo("010-1111-2222")
        assertThat(response.homePhone).isEqualTo("010-1111-2222")
        assertThat(response.role).isEqualTo(AppAuthority.WOMAN)
    }

    @Test
    @DisplayName("중복 사번 -> EmployeeCodeDuplicatedException")
    fun register_duplicate() {
        every { employeeRepository.existsByEmployeeCode("100500") } returns true

        assertThatThrownBy {
            service.register(
                AdminEmployeeManualRegisterRequest(
                    employeeCode = "100500",
                    name = "중복",
                )
            )
        }.isInstanceOf(EmployeeCodeDuplicatedException::class.java)
    }

}
