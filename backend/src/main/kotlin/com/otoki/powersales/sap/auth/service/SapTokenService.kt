package com.otoki.powersales.sap.auth.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.config.SapAuthProperties
import com.otoki.powersales.sap.auth.dto.TokenRequest
import com.otoki.powersales.sap.auth.dto.TokenResponse
import com.otoki.powersales.sap.auth.exception.SapInvalidClientException
import com.otoki.powersales.sap.auth.exception.SapInvalidScopeException
import com.otoki.powersales.sap.auth.exception.SapIpNotAllowedException
import com.otoki.powersales.sap.auth.exception.SapUnsupportedGrantTypeException
import com.otoki.powersales.sap.auth.util.IpAllowlistMatcher
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class SapTokenService(
    private val properties: SapAuthProperties,
    private val auditService: SapInboundAuditService,
    private val jwtCodec: SapJwtCodec
) {

    private val passwordEncoder = BCryptPasswordEncoder()
    private val ipMatcher: IpAllowlistMatcher by lazy { IpAllowlistMatcher(properties.allowedIps) }

    fun issue(request: TokenRequest, clientIp: String): TokenResponse {
        if (!ipMatcher.matches(clientIp)) {
            auditService.record(
                SapInboundAudit(
                    eventType = SapInboundAuditEventType.REQUEST_REJECTED_IP,
                    clientId = request.clientId,
                    endpoint = TOKEN_ENDPOINT,
                    httpMethod = "POST",
                    clientIp = clientIp,
                    reason = "토큰 발급 IP 차단"
                )
            )
            throw SapIpNotAllowedException()
        }

        if (request.grantType != GRANT_TYPE_CLIENT_CREDENTIALS) {
            auditService.record(
                SapInboundAudit(
                    eventType = SapInboundAuditEventType.TOKEN_REJECTED,
                    clientId = request.clientId,
                    endpoint = TOKEN_ENDPOINT,
                    httpMethod = "POST",
                    clientIp = clientIp,
                    reason = "지원하지 않는 grant_type: ${request.grantType}"
                )
            )
            throw SapUnsupportedGrantTypeException()
        }

        val clientId = request.clientId
        val clientSecret = request.clientSecret
        if (clientId.isNullOrBlank() || clientSecret.isNullOrBlank() ||
            clientId != properties.clientId ||
            !passwordEncoder.matches(clientSecret, properties.clientSecretHash)
        ) {
            auditService.record(
                SapInboundAudit(
                    eventType = SapInboundAuditEventType.TOKEN_REJECTED,
                    clientId = clientId,
                    endpoint = TOKEN_ENDPOINT,
                    httpMethod = "POST",
                    clientIp = clientIp,
                    reason = "client_id/secret 불일치"
                )
            )
            throw SapInvalidClientException()
        }

        val requestedScopes = (request.scope ?: "")
            .split(" ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (requestedScopes.isEmpty() || requestedScopes.any { it !in properties.allowedScopes }) {
            auditService.record(
                SapInboundAudit(
                    eventType = SapInboundAuditEventType.TOKEN_REJECTED,
                    clientId = clientId,
                    endpoint = TOKEN_ENDPOINT,
                    httpMethod = "POST",
                    clientIp = clientIp,
                    scope = request.scope,
                    reason = "허용되지 않은 scope"
                )
            )
            throw SapInvalidScopeException()
        }

        val issued = jwtCodec.issue(clientId = clientId, scopes = requestedScopes)
        auditService.record(
            SapInboundAudit(
                eventType = SapInboundAuditEventType.TOKEN_ISSUED,
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
        const val TOKEN_ENDPOINT = "/api/v1/sap/oauth/token"
    }
}
