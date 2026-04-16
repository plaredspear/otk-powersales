package com.otoki.internal.admin.dto.response

import com.otoki.internal.admin.service.PermissionResolveResult
import com.otoki.internal.admin.service.UserPermissionDetail
import com.otoki.internal.sap.entity.Employee

data class EmployeePermissionDetailResponse(
    val employeeId: Long,
    val employeeCode: String,
    val name: String,
    val appAuthority: String?,
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
                appAuthority = employee.appAuthority,
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
    val previousAuthority: String?,
    val newAuthority: String,
    val effectivePermissions: List<String>
)

data class RolePermissionsUpdateResponse(
    val role: String,
    val permissions: List<String>
)
