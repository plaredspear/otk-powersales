package com.otoki.powersales.platform.auth.web

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.platform.common.dto.ApiResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Web Admin 강제 비밀번호 변경 가드.
 *
 * 임시 비밀번호(운영자 리셋)로 로그인한 web 관리자는 비밀번호를 변경하기 전까지 다른 admin API 를
 * 사용할 수 없다. 모바일 [com.otoki.powersales.platform.common.security.PasswordChangeRequiredFilter]
 * 의 web(admin) 대응 필터.
 *
 * - `/api/v1/admin/` 경로 한정 ([WebSecurityConfig] 의 securityMatcher 로 이미 admin 경로로 좁혀져 있으나
 *   방어적으로 재확인).
 * - JWT 클레임 `password_change_required=true` principal 이 화이트리스트(아래 EXEMPT_PATHS) 외 호출 시 403.
 * - 화이트리스트:
 *   - /api/v1/admin/auth/password  (비밀번호 변경 endpoint — 강제 변경을 수행할 통로)
 *   - /api/v1/admin/auth/login
 *   - /api/v1/admin/auth/refresh
 * - 대행(impersonation) 중(`impersonatedBy != null`)이면 통과. 대행자는 대상 사용자의 비밀번호를 변경할 수
 *   없고(변경 endpoint 도 대행 시 차단), 대상의 임시 비밀번호 상태로 대행자 기능이 막히면 안 되기 때문.
 *
 * [WebJwtAuthenticationFilter] 다음, [com.otoki.powersales.admin.security.WebAdminContextFilter]
 * (권한 가드) 앞에서 동작한다 ([WebSecurityConfig]).
 */
class WebPasswordChangeRequiredFilter(
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    companion object {
        private const val ADMIN_PATH_PREFIX = "/api/v1/admin/"

        private val EXEMPT_PATHS = listOf(
            "/api/v1/admin/auth/password",
            "/api/v1/admin/auth/login",
            "/api/v1/admin/auth/refresh"
        )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val requestPath = request.requestURI
        if (!requestPath.startsWith(ADMIN_PATH_PREFIX) || isExemptPath(requestPath)) {
            filterChain.doFilter(request, response)
            return
        }

        val authentication = SecurityContextHolder.getContext().authentication
        if (authentication == null || !authentication.isAuthenticated) {
            filterChain.doFilter(request, response)
            return
        }

        val principal = authentication.principal
        if (principal !is WebUserPrincipal) {
            filterChain.doFilter(request, response)
            return
        }

        // 대행 중에는 대상 사용자의 임시 비밀번호 상태가 대행자 기능을 막지 않도록 통과.
        if (principal.impersonatedBy != null) {
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
