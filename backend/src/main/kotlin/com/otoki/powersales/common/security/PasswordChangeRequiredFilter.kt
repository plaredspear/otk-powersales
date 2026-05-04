package com.otoki.powersales.common.security

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.common.dto.ApiResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 강제 변경 미완료 사원 글로벌 가드 (Spec #584 P1-B §4).
 *
 * - 모바일 경로(/api/v1/mobile/) 에만 적용. admin/web 경로는 통과.
 * - JWT 클레임 password_change_required=true 사원은 화이트리스트(아래 EXEMPT_PATHS) 외 호출 시 403.
 * - 화이트리스트:
 *   - /api/v1/mobile/auth/change-password
 *   - /api/v1/mobile/auth/logout
 *   - /api/v1/mobile/auth/refresh
 *
 * JwtAuthenticationFilter 다음 단계에서 동작한다 (SecurityConfig).
 */
@Component
class PasswordChangeRequiredFilter(
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    companion object {
        private const val MOBILE_PATH_PREFIX = "/api/v1/mobile/"

        private val EXEMPT_PATHS = listOf(
            "/api/v1/mobile/auth/change-password",
            "/api/v1/mobile/auth/logout",
            "/api/v1/mobile/auth/refresh"
        )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestPath = request.requestURI
        if (!requestPath.startsWith(MOBILE_PATH_PREFIX) || isExemptPath(requestPath)) {
            filterChain.doFilter(request, response)
            return
        }

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

        if (!principal.passwordChangeRequired) {
            filterChain.doFilter(request, response)
            return
        }

        response.status = HttpServletResponse.SC_FORBIDDEN
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        val errorResponse = ApiResponse.error<Any>(
            "AUTH_PASSWORD_CHANGE_REQUIRED",
            "비밀번호를 변경해주세요. 임시 비밀번호 상태에서는 다른 기능을 사용할 수 없습니다"
        )
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }

    private fun isExemptPath(requestPath: String): Boolean {
        return EXEMPT_PATHS.any { requestPath == it }
    }
}
