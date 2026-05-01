package com.otoki.powersales.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter

// dev-powersalesapi.otoki.com 은 모바일 클라이언트 전용 (스펙 #542).
// admin path (/api/v1/admin/) 호출은 명시적으로 차단되며, 관리자 web 은 별도 도메인을 사용한다.
class DomainGuardFilter(
    private val apiDomain: String,
    private val adminDomain: String
) : OncePerRequestFilter() {

    companion object {
        // 모바일 클라이언트가 호출하는 path 화이트리스트 + 개발 편의용 swagger/h2.
        // 모바일 전용 prefix(/api/v1/mobile/) 도입(스펙 #574) 이후, endpoint 추가 시에도 본 목록 갱신은 불필요하다.
        private val API_DOMAIN_ALLOWED_PREFIXES = listOf(
            "/api/health",
            "/api/v1/mobile/",
            "/swagger-ui",
            "/v3/api-docs",
            "/h2-console"
        )
        private const val ADMIN_PATH_PREFIX = "/api/v1/admin/"

        private val ADMIN_API_ALLOWED_PREFIXES = listOf(
            "/api/v1/admin/"
        )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val host = (request.getHeader("Host") ?: "").substringBefore(":")
        val path = request.requestURI

        if (apiDomain.isNotEmpty() && host == apiDomain) {
            // 모바일 도메인에서 admin path 는 명시적 차단 (관리자 web 별도 도메인 사용 예정)
            if (path.startsWith(ADMIN_PATH_PREFIX)) {
                response.status = HttpServletResponse.SC_NOT_FOUND
                return
            }
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
