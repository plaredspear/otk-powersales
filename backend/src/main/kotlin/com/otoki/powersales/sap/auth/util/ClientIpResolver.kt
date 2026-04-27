package com.otoki.powersales.sap.auth.util

import jakarta.servlet.http.HttpServletRequest

object ClientIpResolver {

    private val FORWARD_HEADERS = listOf(
        "X-Forwarded-For",
        "X-Real-IP"
    )

    fun resolve(request: HttpServletRequest): String {
        for (header in FORWARD_HEADERS) {
            val value = request.getHeader(header)
            if (!value.isNullOrBlank()) {
                return value.split(",").first().trim()
            }
        }
        return request.remoteAddr ?: ""
    }
}
