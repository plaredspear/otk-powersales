package com.otoki.powersales.external.sap.auth.util

import org.springframework.security.web.util.matcher.IpAddressMatcher

/**
 * CIDR/단일 IP 리스트 기반 매칭. 빈 리스트이면 비활성으로 간주하여 항상 통과.
 */
class IpAllowlistMatcher(allowedCidrs: List<String>) {

    private val matchers: List<IpAddressMatcher> = allowedCidrs
        .filter { it.isNotBlank() }
        .map { IpAddressMatcher(it) }

    val isEnabled: Boolean = matchers.isNotEmpty()

    fun matches(remoteAddress: String?): Boolean {
        if (!isEnabled) return true
        if (remoteAddress.isNullOrBlank()) return false
        return matchers.any { it.matches(remoteAddress) }
    }
}
