package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.request.UpdateAuthorityRequest
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.admin.dto.request.UpdateRolePermissionsRequest
import com.otoki.powersales.admin.dto.request.UpdateUserPermissionsRequest
import com.otoki.powersales.admin.entity.RolePermission
import com.otoki.powersales.admin.entity.UserPermission
import com.otoki.powersales.admin.exception.*
import com.otoki.powersales.admin.repository.RolePermissionRepository
import com.otoki.powersales.admin.repository.UserPermissionRepository
import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminEmployeePermissionService 테스트")
class AdminEmployeePermissionServiceTest {

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var rolePermissionRepository: RolePermissionRepository

    @Mock
    private lateinit var userPermissionRepository: UserPermissionRepository

    @Mock
    private lateinit var adminPermissionResolver: AdminPermissionResolver

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var service: AdminEmployeePermissionService

    private val systemAdmin = Employee(id = 1L, employeeCode = "00000001", name = "시스템관리자", role = UserRole.SYSTEM_ADMIN)
    private val targetEmployee = Employee(id = 2L, employeeCode = "00000002", name = "홍길동", role = UserRole.BRANCH_MANAGER)
    private val nonAdmin = Employee(id = 3L, employeeCode = "00000003", name = "조장", role = UserRole.LEADER)
    private val targetUser = User(id = 102L, username = "홍길동", employeeCode = "00000002", password = "x")

    @BeforeEach
    fun setUp() {
        service = AdminEmployeePermissionService(
            employeeRepository, rolePermissionRepository, userPermissionRepository,
            adminPermissionResolver, userRepository
        )
    }

    @Nested
    @DisplayName("getEmployeePermissions - 사용자 권한 조회")
    inner class GetEmployeePermissionsTests {

        @Test
        @DisplayName("성공 - 시스템관리자가 사원 권한 조회")
        fun getEmployeePermissions_success() {
            // Given
            whenever(employeeRepository.findById(2L)).thenReturn(Optional.of(targetEmployee))
            whenever(adminPermissionResolver.resolveWithDetails(targetEmployee)).thenReturn(
                PermissionResolveResult(
                    rolePermissions = listOf("DASHBOARD_READ"),
                    userPermissions = listOf(UserPermissionDetail("SCHEDULE_WRITE")),
                    effectivePermissions = listOf("DASHBOARD_READ", "SCHEDULE_WRITE")
                )
            )

            // When
            val result = service.getEmployeePermissions(systemAdmin, 2L)

            // Then
            assertThat(result.employeeId).isEqualTo(2L)
            assertThat(result.name).isEqualTo("홍길동")
            assertThat(result.rolePermissions).containsExactly("DASHBOARD_READ")
            assertThat(result.userPermissions).hasSize(1)
            assertThat(result.effectivePermissions).containsExactlyInAnyOrder("DASHBOARD_READ", "SCHEDULE_WRITE")
        }

        @Test
        @DisplayName("실패 - 비관리자 접근 → AdminForbiddenException")
        fun getEmployeePermissions_forbidden() {
            // When & Then — 비관리자 actor 로 호출
            assertThatThrownBy { service.getEmployeePermissions(nonAdmin, 2L) }
                .isInstanceOf(AdminForbiddenException::class.java)
        }

        @Test
        @DisplayName("실패 - 사원 미존재 → EmployeeNotFoundException")
        fun getEmployeePermissions_notFound() {
            // Given
            whenever(employeeRepository.findById(999L)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { service.getEmployeePermissions(systemAdmin, 999L) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("updateUserPermissions - 사용자 직접 권한 수정")
    inner class UpdateUserPermissionsTests {

        @Test
        @DisplayName("성공 - 권한 추가 후 변경된 상태 반환")
        fun updateUserPermissions_success() {
            // Given
            whenever(employeeRepository.findById(2L)).thenReturn(Optional.of(targetEmployee))
            whenever(userRepository.findByEmployeeCode("00000002")).thenReturn(targetUser)
            whenever(userPermissionRepository.save(any<UserPermission>())).thenAnswer { it.arguments[0] }
            whenever(adminPermissionResolver.resolveWithDetails(targetEmployee)).thenReturn(
                PermissionResolveResult(
                    rolePermissions = listOf("DASHBOARD_READ"),
                    userPermissions = listOf(UserPermissionDetail("SCHEDULE_WRITE")),
                    effectivePermissions = listOf("DASHBOARD_READ", "SCHEDULE_WRITE")
                )
            )

            val request = UpdateUserPermissionsRequest(permissions = listOf("SCHEDULE_WRITE"))

            // When
            val result = service.updateUserPermissions(systemAdmin, 2L, request)

            // Then
            assertThat(result.employeeId).isEqualTo(2L)
            assertThat(result.effectivePermissions).contains("SCHEDULE_WRITE")
        }

        @Test
        @DisplayName("실패 - 자기 자신 수정 → CannotModifyOwnPermissionException")
        fun updateUserPermissions_self() {
            // Given
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(systemAdmin))

            val request = UpdateUserPermissionsRequest(permissions = listOf("SCHEDULE_WRITE"))

            // When & Then
            assertThatThrownBy { service.updateUserPermissions(systemAdmin, 1L, request) }
                .isInstanceOf(CannotModifyOwnPermissionException::class.java)
        }

        @Test
        @DisplayName("실패 - 잘못된 권한 → InvalidPermissionException")
        fun updateUserPermissions_invalidPermission() {
            // Given
            whenever(employeeRepository.findById(2L)).thenReturn(Optional.of(targetEmployee))

            val request = UpdateUserPermissionsRequest(permissions = listOf("INVALID_PERM"))

            // When & Then
            assertThatThrownBy { service.updateUserPermissions(systemAdmin, 2L, request) }
                .isInstanceOf(InvalidPermissionException::class.java)
        }

        @Test
        @DisplayName("실패 - 중복 권한 → DuplicatePermissionException")
        fun updateUserPermissions_duplicate() {
            // Given
            whenever(employeeRepository.findById(2L)).thenReturn(Optional.of(targetEmployee))

            val request = UpdateUserPermissionsRequest(permissions = listOf("SCHEDULE_WRITE", "SCHEDULE_WRITE"))

            // When & Then
            assertThatThrownBy { service.updateUserPermissions(systemAdmin, 2L, request) }
                .isInstanceOf(DuplicatePermissionException::class.java)
        }
    }

    @Nested
    @DisplayName("updateAuthority - 역할 변경")
    inner class UpdateAuthorityTests {

        @Test
        @DisplayName("성공 - 지점장 → 영업부장 역할 변경")
        fun updateAuthority_success() {
            // Given
            whenever(employeeRepository.findById(2L)).thenReturn(Optional.of(targetEmployee))
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.arguments[0] }
            whenever(adminPermissionResolver.resolve(targetEmployee)).thenReturn(
                AdminPermission.entries.toSet() - AdminPermission.SCHEDULE_WRITE
            )

            val request = UpdateAuthorityRequest(role = UserRole.SALES_MANAGER)

            // When
            val result = service.updateAuthority(systemAdmin, 2L, request)

            // Then
            assertThat(result.previousRole).isEqualTo("BRANCH_MANAGER")
            assertThat(result.previousRoleLabel).isEqualTo("지점장")
            assertThat(result.newRole).isEqualTo("SALES_MANAGER")
            assertThat(result.newRoleLabel).isEqualTo("영업부장")
        }

        @Test
        @DisplayName("실패 - 자기 자신 역할 변경 → CannotModifyOwnAuthorityException")
        fun updateAuthority_self() {
            // Given
            whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(systemAdmin))

            val request = UpdateAuthorityRequest(role = UserRole.LEADER)

            // When & Then
            assertThatThrownBy { service.updateAuthority(systemAdmin, 1L, request) }
                .isInstanceOf(CannotModifyOwnAuthorityException::class.java)
        }

        @Test
        @DisplayName("실패 - 잘못된 역할 → InvalidAuthorityException")
        fun updateAuthority_invalidRole() {
            // Given
            whenever(employeeRepository.findById(2L)).thenReturn(Optional.of(targetEmployee))

            val request = UpdateAuthorityRequest(role = UserRole.WOMAN)

            // When & Then
            assertThatThrownBy { service.updateAuthority(systemAdmin, 2L, request) }
                .isInstanceOf(InvalidAuthorityException::class.java)
        }
    }

    @Nested
    @DisplayName("updateRolePermissions - 역할별 권한 수정")
    inner class UpdateRolePermissionsTests {

        @Test
        @DisplayName("성공 - 지점장 역할에 SCHEDULE_WRITE 추가")
        fun updateRolePermissions_success() {
            // Given
            whenever(rolePermissionRepository.save(any<RolePermission>())).thenAnswer { it.arguments[0] }

            val request = UpdateRolePermissionsRequest(
                permissions = listOf("DASHBOARD_READ", "SCHEDULE_WRITE")
            )

            // When
            val result = service.updateRolePermissions(systemAdmin, UserRole.BRANCH_MANAGER, request)

            // Then
            assertThat(result.role).isEqualTo("BRANCH_MANAGER")
            assertThat(result.permissions).containsExactlyInAnyOrder("DASHBOARD_READ", "SCHEDULE_WRITE")
        }

        @Test
        @DisplayName("실패 - 잘못된 역할 → InvalidAuthorityException")
        fun updateRolePermissions_invalidRole() {
            // Given

            val request = UpdateRolePermissionsRequest(permissions = listOf("DASHBOARD_READ"))

            // When & Then
            assertThatThrownBy { service.updateRolePermissions(systemAdmin, UserRole.WOMAN, request) }
                .isInstanceOf(InvalidAuthorityException::class.java)
        }

        @Test
        @DisplayName("실패 - 비관리자 → AdminForbiddenException")
        fun updateRolePermissions_forbidden() {
            val request = UpdateRolePermissionsRequest(permissions = listOf("DASHBOARD_READ"))

            // When & Then — 비관리자 actor 로 호출
            assertThatThrownBy { service.updateRolePermissions(nonAdmin, UserRole.BRANCH_MANAGER, request) }
                .isInstanceOf(AdminForbiddenException::class.java)
        }
    }
}
