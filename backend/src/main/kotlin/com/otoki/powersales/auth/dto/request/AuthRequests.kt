package com.otoki.powersales.auth.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * 로그인 요청 DTO
 */
data class LoginRequest(
    @field:NotBlank(message = "사번은 필수입니다")
    @field:Pattern(
        regexp = "^(\\d{8}|ADMIN-[A-Za-z0-9_-]{1,30})$",
        message = "사번은 8자리 숫자 또는 'ADMIN-' 으로 시작하는 관리자 사번이어야 합니다"
    )
    val employeeCode: String,

    @field:NotBlank(message = "비밀번호는 필수입니다")
    @field:Size(min = 4, message = "비밀번호는 4글자 이상이어야 합니다")
    val password: String,

    val deviceId: String? = null
)

/**
 * 비밀번호 변경 요청 DTO (강제/자발 통합).
 *
 * - 강제 변경 (토큰 `passwordChangeRequired=true`): `currentPassword` 무시 (전달되어도 미검증).
 * - 자발 변경: `currentPassword` 필수 — 누락 시 `AUTH_CURRENT_PASSWORD_REQUIRED`, 불일치 시 `AUTH_CURRENT_PASSWORD_MISMATCH`.
 *
 * 신규 비밀번호 정책 검증은 [com.otoki.powersales.auth.policy.PasswordPolicyValidator] 가 권위.
 */
data class ChangePasswordRequest(
    val currentPassword: String? = null,

    @field:NotBlank(message = "새 비밀번호는 필수입니다")
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
 * 비밀번호 검증 요청 DTO (자발 변경 1단계).
 */
data class VerifyPasswordRequest(
    @field:NotBlank(message = "비밀번호를 입력해주세요")
    val currentPassword: String
)
