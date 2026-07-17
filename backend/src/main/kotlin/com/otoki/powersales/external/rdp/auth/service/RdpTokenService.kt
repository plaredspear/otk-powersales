package com.otoki.powersales.external.rdp.auth.service

import com.otoki.powersales.external.rdp.auth.audit.RdpInboundAudit
import com.otoki.powersales.external.rdp.auth.audit.RdpInboundAuditEventType
import com.otoki.powersales.external.rdp.auth.audit.RdpInboundAuditService
import com.otoki.powersales.external.rdp.auth.config.RdpAuthProperties
import com.otoki.powersales.external.rdp.auth.dto.TokenRequest
import com.otoki.powersales.external.rdp.auth.dto.TokenResponse
import com.otoki.powersales.external.rdp.auth.exception.RdpInvalidClientException
import com.otoki.powersales.external.rdp.auth.exception.RdpInvalidScopeException
import com.otoki.powersales.external.rdp.auth.exception.RdpUnsupportedGrantTypeException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class RdpTokenService(
    private val properties: RdpAuthProperties,
    private val auditService: RdpInboundAuditService,
    private val jwtCodec: RdpJwtCodec
) {

    private val passwordEncoder = BCryptPasswordEncoder()

    fun issue(request: TokenRequest, clientIp: String): TokenResponse {
        if (request.grantType != GRANT_TYPE_CLIENT_CREDENTIALS) {
            auditService.record(
                RdpInboundAudit(
                    eventType = RdpInboundAuditEventType.TOKEN_REJECTED,
                    clientId = request.clientId,
                    endpoint = TOKEN_ENDPOINT,
                    httpMethod = "POST",
                    clientIp = clientIp,
                    reason = "지원하지 않는 grant_type: ${request.grantType}"
                )
            )
            throw RdpUnsupportedGrantTypeException()
        }

        val clientId = request.clientId
        val clientSecret = request.clientSecret
        if (clientId.isNullOrBlank() || clientSecret.isNullOrBlank() ||
            clientId != properties.clientId ||
            !passwordEncoder.matches(clientSecret, properties.clientSecretHash)
        ) {
            auditService.record(
                RdpInboundAudit(
                    eventType = RdpInboundAuditEventType.TOKEN_REJECTED,
                    clientId = clientId,
                    endpoint = TOKEN_ENDPOINT,
                    httpMethod = "POST",
                    clientIp = clientIp,
                    reason = "client_id/secret 불일치"
                )
            )
            throw RdpInvalidClientException()
        }

        val requestedScopes = (request.scope ?: "")
            .split(" ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (requestedScopes.isEmpty() || requestedScopes.any { it !in properties.allowedScopes }) {
            auditService.record(
                RdpInboundAudit(
                    eventType = RdpInboundAuditEventType.TOKEN_REJECTED,
                    clientId = clientId,
                    endpoint = TOKEN_ENDPOINT,
                    httpMethod = "POST",
                    clientIp = clientIp,
                    scope = request.scope,
                    reason = "허용되지 않은 scope"
                )
            )
            throw RdpInvalidScopeException()
        }

        val issued = jwtCodec.issue(clientId = clientId, scopes = requestedScopes)
        auditService.record(
            RdpInboundAudit(
                eventType = RdpInboundAuditEventType.TOKEN_ISSUED,
                clientId = clientId,
                endpoint = TOKEN_ENDPOINT,
                httpMethod = "POST",
                clientIp = clientIp,
                scope = requestedScopes.joinToString(" "),
                reason = "jti=${issued.jti}"
            )
        )
        return TokenResponse(
            accessToken = issued.token,
            tokenType = "Bearer",
            expiresIn = issued.expiresIn,
            scope = requestedScopes.joinToString(" ")
        )
    }

    companion object {
        const val GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials"
        const val TOKEN_ENDPOINT = "/api/v1/rdp/oauth/token"
    }
}
