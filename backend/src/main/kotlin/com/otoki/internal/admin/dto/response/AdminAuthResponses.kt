package com.otoki.internal.admin.dto.response

import com.otoki.internal.sap.entity.Employee

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
    val costCenterCode: String?
) {
    companion object {
        fun from(employee: Employee): AdminUserInfo {
            return AdminUserInfo(
                id = employee.id,
                employeeCode = employee.employeeCode,
                name = employee.name,
                orgName = employee.orgName,
                role = employee.role.name,
                appAuthority = employee.appAuthority,
                costCenterCode = employee.costCenterCode
            )
        }
    }
}

data class AdminTokenInfo(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)
