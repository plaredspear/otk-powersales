package com.otoki.internal.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * 로그인 요청 DTO
 */
data class LoginRequest(
    @field:NotBlank(message = "사번은 필수입니다")
    @field:Pattern(regexp = "^\\d{8}$", message = "사번은 8자리 숫자여야 합니다")
    val employeeId: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 4, message = "비밀번호는 4글자 이상이어야 합니다")
    val password: String
)

/**
 * 비밀번호 변경 요청 DTO
 */
data class ChangePasswordRequest(
    @field:NotBlank(message = "현재 비밀번호는 필수입니다")
    val currentPassword: String,

    @field:NotBlank(message = "새 비밀번호는 필수입니다")
    @field:Size(min = 4, message = "비밀번호는 4글자 이상이어야 합니다")
    val newPassword: String
)

/**
 * 토큰 갱신 요청 DTO
 */
data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh Token은 필수입니다")
    val refreshToken: String
)

/**
 * 비밀번호 검증 요청 DTO
 */
data class VerifyPasswordRequest(
    @field:NotBlank(message = "비밀번호를 입력해주세요")
    val password: String
)
