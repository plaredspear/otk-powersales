package com.otoki.internal.auth.dto.response

import com.otoki.internal.sap.entity.Employee

/**
 * 로그인 응답 DTO
 */
data class LoginResponse(
    val user: UserInfo,
    val token: TokenInfo,
    val requiresPasswordChange: Boolean,
    val requiresGpsConsent: Boolean
)

/**
 * 사용자 정보 (로그인 응답에 포함)
 */
data class UserInfo(
    val id: Long,
    val employeeNumber: String,
    val name: String,
    val orgName: String?,
    val role: String
) {
    companion object {
        fun from(employee: Employee): UserInfo {
            return UserInfo(
                id = employee.id,
                employeeNumber = employee.employeeNumber,
                name = employee.name,
                orgName = employee.orgName,
                role = employee.role.name
            )
        }
    }
}

/**
 * 토큰 정보 (로그인 응답에 포함)
 */
data class TokenInfo(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)
