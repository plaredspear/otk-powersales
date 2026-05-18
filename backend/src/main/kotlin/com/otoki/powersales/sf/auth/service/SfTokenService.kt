package com.otoki.powersales.sf.auth.service

import com.otoki.powersales.sf.auth.audit.SfInboundAudit
import com.otoki.powersales.sf.auth.audit.SfInboundAuditEventType
import com.otoki.powersales.sf.auth.audit.SfInboundAuditService
import com.otoki.powersales.sf.auth.config.SfAuthProperties
import com.otoki.powersales.sf.auth.dto.TokenRequest
import com.otoki.powersales.sf.auth.dto.TokenResponse
import com.otoki.powersales.sf.auth.exception.SfInvalidClientException
import com.otoki.powersales.sf.auth.exception.SfInvalidScopeException
import com.otoki.powersales.sf.auth.exception.SfUnsupportedGrantTypeException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class SfTokenService(
    private val properties: SfAuthProperties,
    private val auditService: SfInboundAuditService,
    private val jwtCodec: SfJwtCodec
) {

    private val passwordEncoder = BCryptPasswordEncoder()

    fun issue(request: TokenRequest, clientIp: String): TokenResponse {
        if (request.grantType != GRANT_TYPE_CLIENT_CREDENTIALS) {
            auditService.record(
                SfInboundAudit(
                    eventType = SfInboundAuditEventType.TOKEN_REJECTED,
                    clientId = request.clientId,
                    endpoint = TOKEN_ENDPOINT,
                    httpMethod = "POST",
                    clientIp = clientIp,
                    reason = "지원하지 않는 grant_type: ${request.grantType}"
                )
            )
            throw SfUnsupportedGrantTypeException()
        }

        val clientId = request.clientId
        val clientSecret = request.clientSecret
        if (clientId.isNullOrBlank() || clientSecret.isNullOrBlank() ||
            clientId != properties.clientId ||
            !passwordEncoder.matches(clientSecret, properties.clientSecretHash)
        ) {
            auditService.record(
                SfInboundAudit(
                    eventType = SfInboundAuditEventType.TOKEN_REJECTED,
                    clientId = clientId,
                    endpoint = TOKEN_ENDPOINT,
                    httpMethod = "POST",
                    clientIp = clientIp,
                    reason = "client_id/secret 불일치"
                )
            )
            throw SfInvalidClientException()
        }

        val requestedScopes = (request.scope ?: "")
            .split(" ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (requestedScopes.isEmpty() || requestedScopes.any { it !in properties.allowedScopes }) {
            auditService.record(
                SfInboundAudit(
                    eventType = SfInboundAuditEventType.TOKEN_REJECTED,
                    clientId = clientId,
                    endpoint = TOKEN_ENDPOINT,
                    httpMethod = "POST",
                    clientIp = clientIp,
                    scope = request.scope,
                    reason = "허용되지 않은 scope"
                )
            )
            throw SfInvalidScopeException()
        }

        val issued = jwtCodec.issue(clientId = clientId, scopes = requestedScopes)
        auditService.record(
            SfInboundAudit(
                eventType = SfInboundAuditEventType.TOKEN_ISSUED,
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
        const val TOKEN_ENDPOINT = "/api/v1/sf/oauth/token"
    }
}
