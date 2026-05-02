package com.otoki.powersales.admin.dto.response

import com.otoki.powersales.admin.service.PermissionResolveResult
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.employee.entity.Employee

data class EmployeePermissionDetailResponse(
    val employeeId: Long,
    val employeeCode: String,
    val name: String,
    val role: String?,
    val roleLabel: String?,
    val rolePermissions: List<String>,
    val userPermissions: List<UserPermissionDetailResponse>,
    val effectivePermissions: List<String>
) {
    companion object {
        fun from(employee: Employee, result: PermissionResolveResult): EmployeePermissionDetailResponse {
            return EmployeePermissionDetailResponse(
                employeeId = employee.id,
                employeeCode = employee.employeeCode,
                name = employee.name,
                role = employee.role?.name,
                roleLabel = employee.role?.toKorean(),
                rolePermissions = result.rolePermissions,
                userPermissions = result.userPermissions.map {
                    UserPermissionDetailResponse(
                        permission = it.permission,
                        grantedByName = it.grantedByName
                    )
                },
                effectivePermissions = result.effectivePermissions
            )
        }
    }
}

data class UserPermissionDetailResponse(
    val permission: String,
    val grantedByName: String
)

data class UpdateAuthorityResponse(
    val employeeId: Long,
    val employeeCode: String,
    val name: String,
    val previousRole: String?,
    val previousRoleLabel: String?,
    val newRole: String,
    val newRoleLabel: String,
    val effectivePermissions: List<String>
)

data class RolePermissionsUpdateResponse(
    val role: String,
    val roleLabel: String,
    val permissions: List<String>
) {
    companion object {
        fun of(role: UserRole, permissions: List<String>): RolePermissionsUpdateResponse =
            RolePermissionsUpdateResponse(role = role.name, roleLabel = role.toKorean(), permissions = permissions)
    }
}
