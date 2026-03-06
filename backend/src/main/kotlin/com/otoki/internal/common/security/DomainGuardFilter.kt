package com.otoki.internal.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

class DomainGuardFilter(
    private val apiDomain: String,
    private val adminDomain: String,
    private val sapDomain: String = ""
) : OncePerRequestFilter() {

    companion object {
        private val API_DOMAIN_ALLOWED_PREFIXES = listOf(
            "/api/",
            "/swagger-ui/",
            "/v3/api-docs/",
            "/h2-console/"
        )
        private val ADMIN_API_ALLOWED_PREFIXES = listOf(
            "/api/v1/auth/",
            "/api/v1/admin/"
        )
        private val SAP_DOMAIN_ALLOWED_PREFIXES = listOf(
            "/api/v1/sap/"
        )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val host = (request.getHeader("Host") ?: "").substringBefore(":")
        val path = request.requestURI

        // SAP 전용 도메인이 설정된 경우
        if (sapDomain.isNotEmpty() && host == sapDomain) {
            if (!SAP_DOMAIN_ALLOWED_PREFIXES.any { path.startsWith(it) }) {
                response.status = HttpServletResponse.SC_NOT_FOUND
                return
            }
            filterChain.doFilter(request, response)
            return
        }

        if (apiDomain.isNotEmpty() && host == apiDomain) {
            if (!API_DOMAIN_ALLOWED_PREFIXES.any { path.startsWith(it) }) {
                response.status = HttpServletResponse.SC_NOT_FOUND
                return
            }
        }

        if (adminDomain.isNotEmpty() && host == adminDomain && path.startsWith("/api/")) {
            if (!ADMIN_API_ALLOWED_PREFIXES.any { path.startsWith(it) }) {
                response.status = HttpServletResponse.SC_NOT_FOUND
                return
            }
        }

        filterChain.doFilter(request, response)
    }
}
