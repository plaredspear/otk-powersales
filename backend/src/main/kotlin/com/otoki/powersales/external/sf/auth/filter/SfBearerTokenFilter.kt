package com.otoki.powersales.external.sf.auth.filter

import com.otoki.powersales.external.sf.auth.audit.SfInboundAudit
import com.otoki.powersales.external.sf.auth.audit.SfInboundAuditEventType
import com.otoki.powersales.external.sf.auth.audit.SfInboundAuditService
import com.otoki.powersales.external.sf.auth.service.SfJwtCodec
import com.otoki.powersales.external.sf.auth.service.SfTokenService
import com.otoki.powersales.external.sf.auth.util.ClientIpResolver
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper

class SfBearerTokenFilter(
    private val sfJwtCodec: SfJwtCodec,
    private val auditService: SfInboundAuditService,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI
        if (path == SfTokenService.Companion.TOKEN_ENDPOINT) {
            filterChain.doFilter(request, response)
            return
        }

        val header = request.getHeader("Authorization")
        if (header == null || !header.startsWith("Bearer ")) {
            recordRejection(request, null, null, "Authorization 헤더 누락")
            SfAuthErrorWriter.write(response, objectMapper, 401, "invalid_token", "토큰이 필요합니다")
            return
        }
        val token = header.substring(7)
        if (token.isBlank()) {
            recordRejection(request, null, null, "Bearer 토큰 비어있음")
            SfAuthErrorWriter.write(response, objectMapper, 401, "invalid_token", "토큰이 필요합니다")
            return
        }

        val claims = try {
            sfJwtCodec.parse(token)
        } catch (e: JwtException) {
            recordRejection(request, null, null, "JWT 검증 실패: ${e.javaClass.simpleName}")
            SfAuthErrorWriter.write(response, objectMapper, 401, "invalid_token", "토큰 검증 실패")
            return
        } catch (e: IllegalArgumentException) {
            recordRejection(request, null, null, "JWT 파싱 실패: ${e.javaClass.simpleName}")
            SfAuthErrorWriter.write(response, objectMapper, 401, "invalid_token", "토큰 검증 실패")
            return
        }

        val clientId = claims.subject
        val scopeClaim = claims.get("scope", String::class.java) ?: ""
        val authorities = scopeClaim
            .split(" ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { SimpleGrantedAuthority("SCOPE_$it") }

        val authentication = UsernamePasswordAuthenticationToken(clientId, null, authorities)
        SecurityContextHolder.getContext().authentication = authentication

        filterChain.doFilter(request, response)
    }

    private fun recordRejection(
        request: HttpServletRequest,
        clientId: String?,
        scope: String?,
        reason: String
    ) {
        runCatching {
            auditService.record(
                SfInboundAudit(
                    eventType = SfInboundAuditEventType.REQUEST_REJECTED_AUTH,
                    clientId = clientId,
                    endpoint = request.requestURI,
                    httpMethod = request.method,
                    clientIp = ClientIpResolver.resolve(request),
                    scope = scope,
                    reason = reason
                )
            )
        }
    }
}
