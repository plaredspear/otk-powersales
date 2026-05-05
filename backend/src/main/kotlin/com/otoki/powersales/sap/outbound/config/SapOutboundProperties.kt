package com.otoki.powersales.sap.outbound.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties(prefix = "sap.outbound")
data class SapOutboundProperties(
    val baseUrl: String = "",
    val username: String = "",
    val password: String = "",
    val connectTimeoutMs: Int = 5000,
    val readTimeoutMs: Int = 120000,
    @NestedConfigurationProperty
    val retry: Retry = Retry()
) {
    data class Retry(
        val maxAttempts: Int = 3,
        val delayMs: Long = 1000
    )
}
