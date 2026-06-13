package com.otoki.powersales.external.sap.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "sap.auth")
data class SapAuthProperties(
    val clientId: String = "",
    val clientSecretHash: String = "",
    val jwtSigningKey: String = "",
    val tokenTtlSeconds: Long = 86400,
    val allowedScopes: List<String> = emptyList(),
    val allowedIps: List<String> = emptyList()
)
