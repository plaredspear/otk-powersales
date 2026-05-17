package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.repository.RolePermissionRepository
import com.otoki.powersales.admin.repository.UserPermissionRepository
import com.otoki.powersales.admin.repository.findByRole
import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class UserPermissionDetail(
    val permission: String
)

data class PermissionResolveResult(
    val rolePermissions: List<String>,
    val userPermissions: List<UserPermissionDetail>,
    val effectivePermissions: List<String>
)

@Service
@Transactional(readOnly = true)
class AdminPermissionResolver(
    private val rolePermissionRepository: RolePermissionRepository,
    private val userPermissionRepository: UserPermissionRepository,
    private val userRepository: UserRepository
) {

    fun resolve(employee: Employee): Set<AdminPermission> =
        resolve(employee.role, employee.employeeCode)

    private fun resolve(role: UserRole?, employeeCode: String): Set<AdminPermission> {
        val rolePerms = if (role != null && role != UserRole.UNKNOWN) {
            rolePermissionRepository.findByRole(role)
                .mapNotNull { parsePermission(it.permission) }
                .toSet()
        } else {
            emptySet()
        }

        if (role == UserRole.UNKNOWN) {
            return emptySet()
        }

        val user = userRepository.findByEmployeeCode(employeeCode)
        val userPerms = if (user != null) {
            userPermissionRepository.findByUserId(user.id)
                .mapNotNull { parsePermission(it.permission) }
                .toSet()
        } else {
            emptySet()
        }

        return rolePerms + userPerms
    }

    fun resolveWithDetails(employee: Employee): PermissionResolveResult {
        val role = employee.role
        if (role == UserRole.UNKNOWN) {
            return PermissionResolveResult(
                rolePermissions = emptyList(),
                userPermissions = emptyList(),
                effectivePermissions = emptyList()
            )
        }
        val rolePerms = if (role != null) {
            rolePermissionRepository.findByRole(role)
                .mapNotNull { it.permission.takeIf { p -> parsePermission(p) != null } }
        } else {
            emptyList()
        }

        val user = userRepository.findByEmployeeCode(employee.employeeCode)
        val userPerms = if (user != null) {
            userPermissionRepository.findByUserId(user.id)
                .filter { parsePermission(it.permission) != null }
                .map { UserPermissionDetail(permission = it.permission) }
        } else {
            emptyList()
        }

        val effective = (rolePerms + userPerms.map { it.permission }).distinct()

        return PermissionResolveResult(
            rolePermissions = rolePerms,
            userPermissions = userPerms,
            effectivePermissions = effective
        )
    }

    private fun parsePermission(value: String): AdminPermission? {
        return try {
            AdminPermission.valueOf(value)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
