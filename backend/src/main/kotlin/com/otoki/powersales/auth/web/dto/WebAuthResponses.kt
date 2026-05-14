package com.otoki.powersales.auth.web.dto

import com.otoki.powersales.user.entity.ProfileType

/**
 * Web 로그인 응답 (Spec #760 §5.1).
 */
data class WebLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val passwordChangeRequired: Boolean,
    val user: WebUserSummary
)

/**
 * Web 사용자 요약 — 로그인 응답에 포함되는 User 정보 (Spec #760).
 */
data class WebUserSummary(
    val userId: Long,
    val username: String,
    val name: String?,
    val employeeNumber: String,
    val profileType: ProfileType,
    val isSalesSupport: Boolean
)

/**
 * Web 토큰 갱신 응답 (Spec #760 §5.2).
 */
data class WebTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)

/**
 * Web 비밀번호 변경 응답 (Spec #760 §5.3).
 */
data class WebChangePasswordResponse(
    val passwordChangeRequired: Boolean
)
