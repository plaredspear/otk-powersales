package com.otoki.powersales.auth.dto.response

import com.otoki.powersales.employee.entity.Employee

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
    val employeeCode: String,
    val name: String,
    val orgName: String?,
    val role: String?,
    val roleLabel: String?
) {
    companion object {
        fun from(employee: Employee): UserInfo {
            return UserInfo(
                id = employee.id,
                employeeCode = employee.employeeCode,
                name = employee.name,
                orgName = employee.orgName,
                role = employee.role?.name,
                roleLabel = employee.role?.toKorean()
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
