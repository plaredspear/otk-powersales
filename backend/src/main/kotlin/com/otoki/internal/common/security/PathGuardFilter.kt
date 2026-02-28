package com.otoki.internal.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 허용된 경로 접두사 외의 요청을 Security Filter Chain 전에 즉시 404로 차단.
 * 외부 스캐너/봇의 프로빙 요청이 불필요하게 필터 체인을 타지 않도록 한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class PathGuardFilter : OncePerRequestFilter() {

    companion object {
        private val ALLOWED_PREFIXES = listOf(
            "/api/",
            "/h2-console/",
            "/swagger-ui",
            "/v3/api-docs",
        )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI

        if (ALLOWED_PREFIXES.none { path.startsWith(it) }) {
            response.status = HttpServletResponse.SC_NOT_FOUND
            return
        }

        filterChain.doFilter(request, response)
    }
}
