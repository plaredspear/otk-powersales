package com.otoki.internal.sap.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "sap.auth")
data class SapAuthProperties(
    val apiKey: String = "",
    val allowedIps: String = ""
) {
    fun getAllowedIpList(): List<String> =
        if (allowedIps.isBlank()) emptyList()
        else allowedIps.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
