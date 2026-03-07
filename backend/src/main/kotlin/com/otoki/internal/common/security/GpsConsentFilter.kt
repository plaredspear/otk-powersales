package com.otoki.internal.common.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.common.dto.ApiResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class GpsConsentFilter(
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    companion object {
        private val EXEMPT_PATHS = listOf(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/gps-consent",
            "/api/v1/auth/change-password",
            "/api/v1/auth/logout",
            "/api/v1/health",
            "/api/v1/admin/"
        )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated) {
            filterChain.doFilter(request, response)
            return
        }

        val principal = authentication.principal
        if (principal !is UserPrincipal) {
            filterChain.doFilter(request, response)
            return
        }

        val requestPath = request.requestURI
        if (isExemptPath(requestPath)) {
            filterChain.doFilter(request, response)
            return
        }

        if (!principal.agreementFlag) {
            response.status = HttpServletResponse.SC_FORBIDDEN
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.characterEncoding = "UTF-8"
            val errorResponse = ApiResponse.error<Any>("GPS_CONSENT_REQUIRED", "GPS 사용 동의가 필요합니다")
            response.writer.write(objectMapper.writeValueAsString(errorResponse))
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun isExemptPath(requestPath: String): Boolean {
        return EXEMPT_PATHS.any { requestPath.startsWith(it) }
    }
}
