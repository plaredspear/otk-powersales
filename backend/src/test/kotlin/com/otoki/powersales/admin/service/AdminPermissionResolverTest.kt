package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.entity.RolePermission
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.admin.entity.UserPermission
import com.otoki.powersales.admin.repository.RolePermissionRepository
import com.otoki.powersales.admin.repository.UserPermissionRepository
import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.assertj.core.api.Assertions.assertThat
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
@DisplayName("AdminPermissionResolver 테스트")
class AdminPermissionResolverTest {

    @Mock
    private lateinit var rolePermissionRepository: RolePermissionRepository

    @Mock
    private lateinit var userPermissionRepository: UserPermissionRepository

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @InjectMocks
    private lateinit var resolver: AdminPermissionResolver

    @Nested
    @DisplayName("resolve - 최종 권한 계산")
    inner class ResolveTests {

        @Test
        @DisplayName("역할 권한만 있는 경우 - role_permission 기반 권한 반환")
        fun resolve_roleOnly() {
            // Given
            val employee = createEmployee(role = UserRole.BRANCH_MANAGER)
            whenever(rolePermissionRepository.findByRoleName("BRANCH_MANAGER")).thenReturn(
                listOf(
                    RolePermission(role = "BRANCH_MANAGER", permission = "DASHBOARD_READ"),
                    RolePermission(role = "BRANCH_MANAGER", permission = "SCHEDULE_READ")
                )
            )
            whenever(userPermissionRepository.findByEmployeeId(1L)).thenReturn(emptyList())

            // When
            val result = resolver.resolve(employee)

            // Then
            assertThat(result).containsExactlyInAnyOrder(AdminPermission.DASHBOARD_READ, AdminPermission.SCHEDULE_READ)
        }

        @Test
        @DisplayName("역할 + 개별 권한 합산 - union 결과 반환")
        fun resolve_roleAndUser() {
            // Given
            val employee = createEmployee(role = UserRole.BRANCH_MANAGER)
            whenever(rolePermissionRepository.findByRoleName("BRANCH_MANAGER")).thenReturn(
                listOf(RolePermission(role = "BRANCH_MANAGER", permission = "DASHBOARD_READ"))
            )
            whenever(userPermissionRepository.findByEmployeeId(1L)).thenReturn(
                listOf(UserPermission(employeeId = 1L, permission = "SCHEDULE_WRITE", grantedBy = 2L))
            )

            // When
            val result = resolver.resolve(employee)

            // Then
            assertThat(result).containsExactlyInAnyOrder(AdminPermission.DASHBOARD_READ, AdminPermission.SCHEDULE_WRITE)
        }

        @Test
        @DisplayName("appAuthority가 null - user_permission만 반환")
        fun resolve_nullAuthority() {
            // Given
            val employee = createEmployee(role = null)
            whenever(userPermissionRepository.findByEmployeeId(1L)).thenReturn(
                listOf(UserPermission(employeeId = 1L, permission = "DASHBOARD_READ", grantedBy = 2L))
            )

            // When
            val result = resolver.resolve(employee)

            // Then
            assertThat(result).containsExactly(AdminPermission.DASHBOARD_READ)
        }

        @Test
        @DisplayName("권한 없음 - 빈 Set 반환")
        fun resolve_noPermissions() {
            // Given
            val employee = createEmployee(role = null)
            whenever(userPermissionRepository.findByEmployeeId(1L)).thenReturn(emptyList())

            // When
            val result = resolver.resolve(employee)

            // Then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("resolveWithDetails - 상세 권한 결과")
    inner class ResolveWithDetailsTests {

        @Test
        @DisplayName("역할 + 개별 권한 상세 - grantedByName 포함")
        fun resolveWithDetails_success() {
            // Given
            val employee = createEmployee(role = UserRole.BRANCH_MANAGER)
            val granter = Employee(id = 2L, employeeCode = "00000002", name = "관리자김")
            whenever(rolePermissionRepository.findByRoleName("BRANCH_MANAGER")).thenReturn(
                listOf(RolePermission(role = "BRANCH_MANAGER", permission = "DASHBOARD_READ"))
            )
            whenever(userPermissionRepository.findByEmployeeId(1L)).thenReturn(
                listOf(UserPermission(employeeId = 1L, permission = "SCHEDULE_WRITE", grantedBy = 2L))
            )
            whenever(employeeRepository.findAllById(listOf(2L))).thenReturn(listOf(granter))

            // When
            val result = resolver.resolveWithDetails(employee)

            // Then
            assertThat(result.rolePermissions).containsExactly("DASHBOARD_READ")
            assertThat(result.userPermissions).hasSize(1)
            assertThat(result.userPermissions[0].permission).isEqualTo("SCHEDULE_WRITE")
            assertThat(result.userPermissions[0].grantedByName).isEqualTo("관리자김")
            assertThat(result.effectivePermissions).containsExactlyInAnyOrder("DASHBOARD_READ", "SCHEDULE_WRITE")
        }
    }

    private fun createEmployee(
        id: Long = 1L,
        role: UserRole? = UserRole.LEADER
    ): Employee {
        return Employee(id = id, employeeCode = "00000001", name = "테스트", role = role)
    }
}
