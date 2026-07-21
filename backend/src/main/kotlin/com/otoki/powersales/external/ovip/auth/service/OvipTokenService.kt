package com.otoki.powersales.external.ovip.auth.service

import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAudit
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAuditEventType
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAuditService
import com.otoki.powersales.external.ovip.auth.config.OvipAuthProperties
import com.otoki.powersales.external.ovip.auth.dto.TokenRequest
import com.otoki.powersales.external.ovip.auth.dto.TokenResponse
import com.otoki.powersales.external.ovip.auth.exception.OvipInvalidClientException
import com.otoki.powersales.external.ovip.auth.exception.OvipUnsupportedGrantTypeException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class OvipTokenService(
    private val properties: OvipAuthProperties,
    private val auditService: OvipInboundAuditService,
    private val jwtCodec: OvipJwtCodec
) {

    private val passwordEncoder = BCryptPasswordEncoder()

    fun issue(request: TokenRequest, clientIp: String): TokenResponse {
        if (request.grantType != GRANT_TYPE_CLIENT_CREDENTIALS) {
            auditService.record(
                OvipInboundAudit(
                    eventType = OvipInboundAuditEventType.TOKEN_REJECTED,
                    clientId = request.clientId,
                    endpoint = TOKEN_ENDPOINT,
                    httpMethod = "POST",
                    clientIp = clientIp,
                    reason = "지원하지 않는 grant_type: ${request.grantType}"
                )
            )
            throw OvipUnsupportedGrantTypeException()
        }

        val clientId = request.clientId
        val clientSecret = request.clientSecret
        if (clientId.isNullOrBlank() || clientSecret.isNullOrBlank() ||
            clientId != properties.clientId ||
            !passwordEncoder.matches(clientSecret, properties.clientSecretHash)
        ) {
            auditService.record(
                OvipInboundAudit(
                    eventType = OvipInboundAuditEventType.TOKEN_REJECTED,
                    clientId = clientId,
                    endpoint = TOKEN_ENDPOINT,
                    httpMethod = "POST",
                    clientIp = clientIp,
                    reason = "client_id/secret 불일치"
                )
            )
            throw OvipInvalidClientException()
        }

        // scope 권한 체크는 수행하지 않는다. 요청 scope 는 검증 없이 그대로 토큰/응답에 반영만 한다
        // (조회 API 의 @PreAuthorize scope 가드도 제거됨 — 유효한 client 인증만 통과하면 조회 허용).
        val requestedScopes = (request.scope ?: "")
            .split(" ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val issued = jwtCodec.issue(clientId = clientId, scopes = requestedScopes)
        auditService.record(
            OvipInboundAudit(
                eventType = OvipInboundAuditEventType.TOKEN_ISSUED,
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
        const val TOKEN_ENDPOINT = "/api/v1/ovip/oauth/token"
    }
}
