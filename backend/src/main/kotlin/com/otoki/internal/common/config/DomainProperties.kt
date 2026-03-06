package com.otoki.internal.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.domain")
data class DomainProperties(
    val api: String = "",
    val admin: String = "",
    val sap: String = ""
)
