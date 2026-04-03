package com.otoki.internal.admin.service

import com.otoki.internal.admin.security.AdminPermission
import com.otoki.internal.auth.exception.EmployeeNotFoundException
import com.otoki.internal.sap.entity.Employee
import com.otoki.internal.sap.repository.EmployeeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminPermissionMatrixService 테스트")
class AdminPermissionMatrixServiceTest {

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @InjectMocks
    private lateinit var service: AdminPermissionMatrixService

    @Nested
    @DisplayName("getMatrix - 역할-권한 매트릭스 조회")
    inner class GetMatrixTests {

        @Test
        @DisplayName("성공 - 조장 역할 사용자 → permissions, roles, currentUser 모두 포함")
        fun getMatrix_success() {
            // Given
            val employee = createEmployee(appAuthority = "조장")
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))

            // When
            val result = service.getMatrix(1L)

            // Then
            assertThat(result.permissions).hasSize(AdminPermission.entries.size)
            assertThat(result.permissions[0].code).isNotBlank()
            assertThat(result.permissions[0].description).isNotBlank()
            assertThat(result.permissions[0].menus).isNotEmpty()

            assertThat(result.roles).hasSize(7)
            assertThat(result.roles.map { it.role }).contains("시스템관리자", "조장", "지점장", "영업부장")

            assertThat(result.currentUser.role).isEqualTo("조장")
            assertThat(result.currentUser.permissions).hasSize(AdminPermission.entries.size)
        }

        @Test
        @DisplayName("성공 - Permission 상세 정보에 code, description, menus 포함")
        fun getMatrix_permissionDetails() {
            // Given
            val employee = createEmployee(appAuthority = "조장")
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))

            // When
            val result = service.getMatrix(1L)

            // Then
            val dashboardRead = result.permissions.find { it.code == "DASHBOARD_READ" }
            assertThat(dashboardRead).isNotNull
            assertThat(dashboardRead!!.description).isEqualTo("대시보드 조회")
            assertThat(dashboardRead.menus).containsExactly("대시보드")
        }

        @Test
        @DisplayName("성공 - 지점장 역할 → SCHEDULE_WRITE 미포함")
        fun getMatrix_limitedPermissions() {
            // Given
            val employee = createEmployee(appAuthority = "지점장")
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))

            // When
            val result = service.getMatrix(1L)

            // Then
            assertThat(result.currentUser.role).isEqualTo("지점장")
            assertThat(result.currentUser.permissions).doesNotContain("SCHEDULE_WRITE")
            assertThat(result.currentUser.permissions).contains("SCHEDULE_READ")
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자 → EmployeeNotFoundException")
        fun getMatrix_employeeNotFound() {
            // Given
            whenever(employeeRepository.findById(999L)).thenReturn(Optional.empty())

            // Then
            assertThatThrownBy { service.getMatrix(999L) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }
    }

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "00000001",
        appAuthority: String? = "조장"
    ): Employee {
        return Employee(
            id = id,
            employeeCode = employeeCode,
            name = "테스트사원",
            appAuthority = appAuthority
        )
    }
}
