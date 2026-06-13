package com.otoki.powersales.platform.auth.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Web 로그인 요청 (Spec #760).
 */
data class WebLoginRequest(
    @field:NotBlank(message = "username 은 필수입니다")
    @field:Size(max = 80, message = "username 은 80자를 초과할 수 없습니다")
    val username: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 4, message = "비밀번호는 4글자 이상이어야 합니다")
    val password: String
)

/**
 * Web Refresh Token 요청 (Spec #760).
 */
data class WebRefreshTokenRequest(
    @field:NotBlank(message = "refreshToken 은 필수입니다")
    val refreshToken: String
)

/**
 * Web 비밀번호 변경 요청 (Spec #760).
 */
data class WebChangePasswordRequest(
    val currentPassword: String?,

    @field:NotBlank(message = "새 비밀번호는 필수입니다")
    @field:Size(min = 4, message = "새 비밀번호는 4글자 이상이어야 합니다")
    val newPassword: String
)
