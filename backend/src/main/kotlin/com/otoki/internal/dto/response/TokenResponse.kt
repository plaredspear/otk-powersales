package com.otoki.internal.dto.response

/**
 * 토큰 갱신 응답 DTO
 */
data class TokenResponse(
    val accessToken: String,
    val expiresIn: Int
)
