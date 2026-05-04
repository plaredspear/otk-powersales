package com.otoki.powersales.auth.dto.response

/**
 * 비밀번호 변경 응답 DTO (Spec #584 P1-B §3.2).
 *
 * 변경 후 새 토큰 페어를 발급한다 (클레임의 `passwordChangeRequired=false` 반영).
 * 기존 토큰은 만료 시점까지 자연 무효화된다.
 */
data class ChangePasswordResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)
