package com.otoki.powersales.admin.dto.response

import com.otoki.powersales.admin.service.AdminPermissionResolver
import com.otoki.powersales.sap.entity.Employee

data class AdminLoginResponse(
    val user: AdminUserInfo,
    val token: AdminTokenInfo
)

data class AdminUserInfo(
    val id: Long,
    val employeeCode: String,
    val name: String,
    val orgName: String?,
    val role: String,
    val appAuthority: String?,
    val costCenterCode: String?,
    val permissions: List<String>
) {
    companion object {
        fun from(employee: Employee, permissionResolver: AdminPermissionResolver): AdminUserInfo {
            val permissionNames = permissionResolver.resolve(employee)
                .map { it.name }
            return AdminUserInfo(
                id = employee.id,
                employeeCode = employee.employeeCode,
                name = employee.name,
                orgName = employee.orgName,
                role = employee.role.name,
                appAuthority = employee.appAuthority,
                costCenterCode = employee.costCenterCode,
                permissions = permissionNames
            )
        }
    }
}

data class AdminTokenInfo(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)
