package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.repository.RolePermissionRepository
import com.otoki.powersales.admin.repository.UserPermissionRepository
import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.sap.entity.Employee
import com.otoki.powersales.sap.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class UserPermissionDetail(
    val permission: String,
    val grantedByName: String
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
    private val employeeRepository: EmployeeRepository
) {

    fun resolve(employee: Employee): Set<AdminPermission> {
        val rolePerms = if (employee.appAuthority != null) {
            rolePermissionRepository.findByRole(employee.appAuthority!!)
                .mapNotNull { parsePermission(it.permission) }
                .toSet()
        } else {
            emptySet()
        }

        val userPerms = userPermissionRepository.findByEmployeeId(employee.id)
            .mapNotNull { parsePermission(it.permission) }
            .toSet()

        return rolePerms + userPerms
    }

    fun resolveWithDetails(employee: Employee): PermissionResolveResult {
        val rolePerms = if (employee.appAuthority != null) {
            rolePermissionRepository.findByRole(employee.appAuthority!!)
                .mapNotNull { it.permission.takeIf { p -> parsePermission(p) != null } }
        } else {
            emptyList()
        }

        val userPermEntities = userPermissionRepository.findByEmployeeId(employee.id)
        val grantedByIds = userPermEntities.map { it.grantedBy }.distinct()
        val grantedByNames = if (grantedByIds.isNotEmpty()) {
            employeeRepository.findAllById(grantedByIds).associate { it.id to it.name }
        } else {
            emptyMap()
        }

        val userPerms = userPermEntities
            .filter { parsePermission(it.permission) != null }
            .map { up ->
                UserPermissionDetail(
                    permission = up.permission,
                    grantedByName = grantedByNames[up.grantedBy] ?: ""
                )
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
