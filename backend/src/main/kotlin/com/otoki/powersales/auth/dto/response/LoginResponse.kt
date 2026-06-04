package com.otoki.powersales.auth.dto.response

import com.otoki.powersales.employee.entity.Employee

/**
 * 로그인 응답 DTO
 */
data class LoginResponse(
    val user: UserInfo,
    val token: TokenInfo,
    val passwordChangeRequired: Boolean,
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
    /** SF DKRetail__AppAuthority__c picklist value (`여사원` / `조장` / `지점장` / `AccountViewAll`) 또는 null. */
    val role: String?
) {
    companion object {
        fun from(employee: Employee): UserInfo {
            return UserInfo(
                id = employee.id,
                employeeCode = employee.employeeCode ?: error("로그인 사원의 사번이 null - 비정상"),
                name = employee.name,
                orgName = employee.orgName,
                role = employee.role
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
