package com.otoki.powersales.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * JWT 인증 필터
 * 모든 요청에서 Authorization 헤더의 Bearer 토큰을 검증
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = resolveToken(request)

        if (token != null) {
            if (jwtTokenProvider.validateToken(token)) {
                val tokenType = jwtTokenProvider.getTokenType(token)
                val audience = jwtTokenProvider.getAudienceFromToken(token)
                // Spec #760 — Mobile FilterChain 은 audience="web" 토큰을 거부 (cross-platform 차단)
                if (audience == JwtTokenProvider.AUDIENCE_WEB) {
                    request.setAttribute("jwt.audienceMismatch", true)
                } else if (tokenType == "access") {
                    val userId = jwtTokenProvider.getUserIdFromToken(token)
                    val role = jwtTokenProvider.getRoleFromToken(token)
                    if (role == null) {
                        // 신규 enum 매핑 실패 (구 토큰의 USER/ADMIN 등) — 인증 미설정 → 401 처리
                        request.setAttribute("jwt.invalidRole", true)
                    } else {
                        val agreementFlag = jwtTokenProvider.getAgreementFlagFromToken(token)
                        val passwordChangeRequired = jwtTokenProvider.getPasswordChangeRequiredFromToken(token)
                        val principal = UserPrincipal(userId, role, agreementFlag, passwordChangeRequired)

                        val authentication = UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.authorities
                        )
                        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = authentication
                    }
                }
            } else if (jwtTokenProvider.isTokenExpired(token)) {
                request.setAttribute("jwt.expired", true)
            }
        }

        filterChain.doFilter(request, response)
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}
