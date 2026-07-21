package com.otoki.powersales.external.ovip.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ovip.auth")
data class OvipAuthProperties(
    val clientId: String = "",
    val clientSecretHash: String = "",
    val jwtSigningKey: String = "",
    val tokenTtlSeconds: Long = 86400
)
