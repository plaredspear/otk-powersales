package com.otoki.internal.admin.dto.response

import com.otoki.internal.sap.entity.User

data class AdminLoginResponse(
    val user: AdminUserInfo,
    val token: AdminTokenInfo
)

data class AdminUserInfo(
    val id: Long,
    val employeeId: String,
    val name: String,
    val orgName: String?,
    val role: String,
    val appAuthority: String?,
    val costCenterCode: String?
) {
    companion object {
        fun from(user: User): AdminUserInfo {
            return AdminUserInfo(
                id = user.id,
                employeeId = user.employeeId,
                name = user.name,
                orgName = user.orgName,
                role = user.role.name,
                appAuthority = user.appAuthority,
                costCenterCode = user.costCenterCode
            )
        }
    }
}

data class AdminTokenInfo(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)
