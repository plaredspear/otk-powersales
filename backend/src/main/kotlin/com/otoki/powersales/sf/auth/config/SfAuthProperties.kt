package com.otoki.powersales.sf.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "sf.auth")
data class SfAuthProperties(
    val clientId: String = "",
    val clientSecretHash: String = "",
    val jwtSigningKey: String = "",
    val tokenTtlSeconds: Long = 86400,
    val allowedScopes: List<String> = emptyList()
)
