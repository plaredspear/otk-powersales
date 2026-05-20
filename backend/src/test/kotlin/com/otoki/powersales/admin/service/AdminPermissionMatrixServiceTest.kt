package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.entity.RolePermission
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.admin.repository.RolePermissionRepository
import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.*

@DisplayName("AdminPermissionMatrixService 테스트")
class AdminPermissionMatrixServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val rolePermissionRepository: RolePermissionRepository = mockk()
    private val adminPermissionResolver: AdminPermissionResolver = mockk()
    private val service = AdminPermissionMatrixService(employeeRepository, rolePermissionRepository, adminPermissionResolver)

    @Nested
    @DisplayName("getMatrix - 역할-권한 매트릭스 조회")
    inner class GetMatrixTests {

        @Test
        @DisplayName("성공 - 조장 역할 사용자 → permissions, roles, currentUser 모두 포함")
        fun getMatrix_success() {
            // Given
            val employee = createEmployee(role = UserRole.LEADER)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { rolePermissionRepository.findAll() } returns createAllRolePermissions()
            every { adminPermissionResolver.resolve(employee) } returns AdminPermission.entries.toSet()

            // When
            val result = service.getMatrix(1L)

            // Then
            assertThat(result.permissions).hasSize(AdminPermission.entries.size)
            assertThat(result.permissions[0].code).isNotBlank()
            assertThat(result.permissions[0].description).isNotBlank()
            assertThat(result.permissions[0].menus).isNotEmpty()

            assertThat(result.roles).isNotEmpty()

            assertThat(result.currentUser.role).isEqualTo("LEADER")
            assertThat(result.currentUser.roleLabel).isEqualTo("조장")
            assertThat(result.currentUser.permissions).hasSize(AdminPermission.entries.size)
            assertThat(result.currentUser.canManagePermissions).isFalse()
        }

        @Test
        @DisplayName("성공 - 시스템관리자 → canManagePermissions=true")
        fun getMatrix_systemAdmin() {
            // Given
            val employee = createEmployee(role = UserRole.SYSTEM_ADMIN)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { rolePermissionRepository.findAll() } returns emptyList()
            every { adminPermissionResolver.resolve(employee) } returns AdminPermission.entries.toSet()

            // When
            val result = service.getMatrix(1L)

            // Then
            assertThat(result.currentUser.canManagePermissions).isTrue()
        }

        @Test
        @DisplayName("성공 - Permission 상세 정보에 code, description, menus 포함")
        fun getMatrix_permissionDetails() {
            // Given
            val employee = createEmployee(role = UserRole.LEADER)
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { rolePermissionRepository.findAll() } returns emptyList()
            every { adminPermissionResolver.resolve(employee) } returns AdminPermission.entries.toSet()

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
            val employee = createEmployee(role = UserRole.BRANCH_MANAGER)
            val limitedPerms = AdminPermission.entries.toSet() - AdminPermission.SCHEDULE_WRITE
            every { employeeRepository.findById(1L) } returns Optional.of(employee)
            every { rolePermissionRepository.findAll() } returns emptyList()
            every { adminPermissionResolver.resolve(employee) } returns limitedPerms

            // When
            val result = service.getMatrix(1L)

            // Then
            assertThat(result.currentUser.role).isEqualTo("BRANCH_MANAGER")
            assertThat(result.currentUser.roleLabel).isEqualTo("지점장")
            assertThat(result.currentUser.permissions).doesNotContain("SCHEDULE_WRITE")
            assertThat(result.currentUser.permissions).contains("SCHEDULE_READ")
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자 → EmployeeNotFoundException")
        fun getMatrix_employeeNotFound() {
            // Given
            every { rolePermissionRepository.findAll() } returns emptyList()
            every { employeeRepository.findById(999L) } returns Optional.empty()

            // Then
            assertThatThrownBy { service.getMatrix(999L) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }
    }

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "00000001",
        role: UserRole? = UserRole.LEADER
    ): Employee {
        return Employee(
            id = id,
            employeeCode = employeeCode,
            name = "테스트사원",
            role = role
        )
    }

    private fun createAllRolePermissions(): List<RolePermission> {
        val roles = listOf("SYSTEM_ADMIN", "LEADER", "BRANCH_MANAGER", "SALES_MANAGER")
        return roles.flatMap { role ->
            AdminPermission.entries.map { perm ->
                RolePermission(role = role, permission = perm.name)
            }
        }
    }
}
