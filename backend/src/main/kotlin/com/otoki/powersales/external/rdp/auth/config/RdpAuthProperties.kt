package com.otoki.powersales.external.rdp.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "rdp.auth")
data class RdpAuthProperties(
    val clientId: String = "",
    val clientSecretHash: String = "",
    val jwtSigningKey: String = "",
    val tokenTtlSeconds: Long = 86400,
    val allowedScopes: List<String> = emptyList()
)
