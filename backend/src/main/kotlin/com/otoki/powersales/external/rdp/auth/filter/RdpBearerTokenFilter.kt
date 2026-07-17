package com.otoki.powersales.external.rdp.auth.filter

import com.otoki.powersales.external.rdp.auth.audit.RdpInboundAudit
import com.otoki.powersales.external.rdp.auth.audit.RdpInboundAuditEventType
import com.otoki.powersales.external.rdp.auth.audit.RdpInboundAuditService
import com.otoki.powersales.external.rdp.auth.service.RdpJwtCodec
import com.otoki.powersales.external.rdp.auth.service.RdpTokenService
import com.otoki.powersales.external.rdp.auth.util.ClientIpResolver
import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper

class RdpBearerTokenFilter(
    private val rdpJwtCodec: RdpJwtCodec,
    private val auditService: RdpInboundAuditService,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI
        if (path == RdpTokenService.Companion.TOKEN_ENDPOINT) {
            filterChain.doFilter(request, response)
            return
        }

        val header = request.getHeader("Authorization")
        if (header == null || !header.startsWith("Bearer ")) {
            recordRejection(request, null, null, "Authorization 헤더 누락")
            RdpAuthErrorWriter.write(response, objectMapper, 401, "invalid_token", "토큰이 필요합니다")
            return
        }
        val token = header.substring(7)
        if (token.isBlank()) {
            recordRejection(request, null, null, "Bearer 토큰 비어있음")
            RdpAuthErrorWriter.write(response, objectMapper, 401, "invalid_token", "토큰이 필요합니다")
            return
        }

        val claims = try {
            rdpJwtCodec.parse(token)
        } catch (e: JwtException) {
            recordRejection(request, null, null, "JWT 검증 실패: ${e.javaClass.simpleName}")
            RdpAuthErrorWriter.write(response, objectMapper, 401, "invalid_token", "토큰 검증 실패")
            return
        } catch (e: IllegalArgumentException) {
            recordRejection(request, null, null, "JWT 파싱 실패: ${e.javaClass.simpleName}")
            RdpAuthErrorWriter.write(response, objectMapper, 401, "invalid_token", "토큰 검증 실패")
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
                RdpInboundAudit(
                    eventType = RdpInboundAuditEventType.REQUEST_REJECTED_AUTH,
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
