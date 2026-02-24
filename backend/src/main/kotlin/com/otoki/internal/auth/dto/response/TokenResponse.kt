package com.otoki.internal.auth.dto.response

/**
 * 토큰 갱신 응답 DTO
 */
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)
