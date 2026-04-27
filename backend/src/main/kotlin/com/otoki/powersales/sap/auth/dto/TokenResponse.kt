package com.otoki.powersales.sap.auth.dto

data class TokenResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Int,
    val scope: String
)
