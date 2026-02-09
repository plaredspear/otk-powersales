package com.otoki.internal.dto.response

import com.otoki.internal.entity.User

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
    val employeeId: String,
    val name: String,
    val department: String,
    val branchName: String,
    val role: String
) {
    companion object {
        fun from(user: User): UserInfo {
            return UserInfo(
                id = user.id,
                employeeId = user.employeeId,
                name = user.name,
                department = user.department,
                branchName = user.branchName,
                role = user.role.name
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
