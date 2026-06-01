package com.otoki.powersales.auth.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Web JWT 인증 필터 (Spec #760).
 *
 * `/api/v1/admin/` 하위 경로 요청에서 Authorization Bearer JWT 를 파싱하여 [WebUserPrincipal] 복원.
 * audience claim 이 `"web"` 아니면 인증 미설정 → 후속 401 처리.
 *
 * 토큰만으로 principal 복원 — DB 재조회 없음 (성능 + DB 의존 회피).
 * 권한(authorities) 은 token claim 의 profile_name + is_sales_support 로 재계산.
 *
 * principal.permissions 는 본 필터에서 빈 set 으로 박는다. SF 권한 가드 평가는
 * [com.otoki.powersales.admin.security.WebAdminContextFilter] 가 AdminPermissionCache 로
 * lazy lookup (JWT claim 비대화 회피 — spec #808 expandAllDataBits 일반화 이후 token byte 가
 * 8KB 초과해 nginx large_client_header_buffers 한도를 초과한 사례 해소).
 */
class WebJwtAuthenticationFilter(
    private val webJwtService: WebJwtService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = resolveToken(request)

        if (token != null) {
            if (webJwtService.validateAccessToken(token)) {
                try {
                    val userId = webJwtService.getUserIdFromToken(token)
                    val username = webJwtService.getUsernameFromToken(token)
                    val employeeCode = webJwtService.getEmployeeCodeFromToken(token)
                    val employeeId = webJwtService.getEmployeeIdFromToken(token)
                    val costCenterCode = webJwtService.getCostCenterCodeFromToken(token)
                    val profileName: String? = webJwtService.getProfileNameFromToken(token)
                    val isSalesSupport = webJwtService.getIsSalesSupportFromToken(token)
                    val passwordChangeRequired = webJwtService.getPasswordChangeRequiredFromToken(token)
                    val role: String? = webJwtService.getRoleFromToken(token)
                    val impersonatedBy: Long? = webJwtService.getImpersonatedByFromToken(token)

                    val authorities = WebUserDetailsService.resolveAuthoritiesByProfileName(profileName, isSalesSupport)
                    val principal = WebUserPrincipal(
                        userId = userId,
                        usernameValue = username,
                        employeeCode = employeeCode,
                        employeeId = employeeId,
                        role = role,
                        costCenterCode = costCenterCode,
                        profileName = profileName,
                        profileId = null,
                        isSalesSupport = isSalesSupport,
                        passwordChangeRequired = passwordChangeRequired,
                        permissions = emptySet(),
                        impersonatedBy = impersonatedBy,
                        encodedPassword = "",
                        grantedAuthorities = authorities,
                        active = true
                    )
                    val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                } catch (_: Exception) {
                    request.setAttribute("jwt.invalidRole", true)
                }
            } else if (webJwtService.isTokenExpired(token)) {
                request.setAttribute("jwt.expired", true)
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}
