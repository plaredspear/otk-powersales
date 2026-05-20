package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.entity.RolePermission
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.admin.entity.UserPermission
import com.otoki.powersales.admin.repository.RolePermissionRepository
import com.otoki.powersales.admin.repository.UserPermissionRepository
import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AdminPermissionResolver 테스트")
class AdminPermissionResolverTest {

    private val rolePermissionRepository: RolePermissionRepository = mockk()
    private val userPermissionRepository: UserPermissionRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val resolver = AdminPermissionResolver(rolePermissionRepository, userPermissionRepository, userRepository)

    private val targetUser = User(id = 101L, username = "테스트", employeeCode = "00000001", password = "x")

    @Nested
    @DisplayName("resolve - 최종 권한 계산")
    inner class ResolveTests {

        @Test
        @DisplayName("역할 권한만 있는 경우 - role_permission 기반 권한 반환")
        fun resolve_roleOnly() {
            // Given
            val employee = createEmployee(role = UserRole.BRANCH_MANAGER)
            every { rolePermissionRepository.findByRoleName("BRANCH_MANAGER") } returns listOf(
                RolePermission(role = "BRANCH_MANAGER", permission = "DASHBOARD_READ"),
                RolePermission(role = "BRANCH_MANAGER", permission = "SCHEDULE_READ")
            )
            every { userRepository.findByEmployeeCode("00000001") } returns targetUser
            every { userPermissionRepository.findByUserId(101L) } returns emptyList()

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
            every { rolePermissionRepository.findByRoleName("BRANCH_MANAGER") } returns listOf(
                RolePermission(role = "BRANCH_MANAGER", permission = "DASHBOARD_READ")
            )
            every { userRepository.findByEmployeeCode("00000001") } returns targetUser
            every { userPermissionRepository.findByUserId(101L) } returns listOf(
                UserPermission(userId = 101L, permission = "SCHEDULE_WRITE")
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
            every { userRepository.findByEmployeeCode("00000001") } returns targetUser
            every { userPermissionRepository.findByUserId(101L) } returns listOf(
                UserPermission(userId = 101L, permission = "DASHBOARD_READ")
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
            every { userRepository.findByEmployeeCode("00000001") } returns targetUser
            every { userPermissionRepository.findByUserId(101L) } returns emptyList()

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
        @DisplayName("역할 + 개별 권한 상세")
        fun resolveWithDetails_success() {
            // Given
            val employee = createEmployee(role = UserRole.BRANCH_MANAGER)
            every { rolePermissionRepository.findByRoleName("BRANCH_MANAGER") } returns listOf(
                RolePermission(role = "BRANCH_MANAGER", permission = "DASHBOARD_READ")
            )
            every { userRepository.findByEmployeeCode("00000001") } returns targetUser
            every { userPermissionRepository.findByUserId(101L) } returns listOf(
                UserPermission(userId = 101L, permission = "SCHEDULE_WRITE")
            )

            // When
            val result = resolver.resolveWithDetails(employee)

            // Then
            assertThat(result.rolePermissions).containsExactly("DASHBOARD_READ")
            assertThat(result.userPermissions).hasSize(1)
            assertThat(result.userPermissions[0].permission).isEqualTo("SCHEDULE_WRITE")
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
