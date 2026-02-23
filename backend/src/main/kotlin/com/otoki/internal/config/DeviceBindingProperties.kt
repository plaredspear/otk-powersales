package com.otoki.internal.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "device.binding")
data class DeviceBindingProperties(
    val enabled: Boolean = true,
    val excludedIds: String = ""
) {
    fun isExcluded(employeeId: String): Boolean {
        if (excludedIds.isBlank()) return false
        return excludedIds.split(",").map { it.trim() }.contains(employeeId)
    }
}
