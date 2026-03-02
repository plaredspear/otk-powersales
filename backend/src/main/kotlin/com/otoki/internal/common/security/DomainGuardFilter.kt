package com.otoki.internal.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

class DomainGuardFilter(
    private val apiDomain: String,
    private val adminDomain: String
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val host = (request.getHeader("Host") ?: "").substringBefore(":")
        val path = request.requestURI

        if (apiDomain.isNotEmpty() && host == apiDomain && path.startsWith("/admin/")) {
            response.status = HttpServletResponse.SC_NOT_FOUND
            return
        }

        if (adminDomain.isNotEmpty() && host == adminDomain && path.startsWith("/api/")) {
            response.status = HttpServletResponse.SC_NOT_FOUND
            return
        }

        filterChain.doFilter(request, response)
    }
}
