package com.otoki.powersales.external.sap.auth.service

import com.otoki.powersales.external.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.external.sap.auth.config.SapAuthProperties
import com.otoki.powersales.external.sap.auth.dto.TokenRequest
import com.otoki.powersales.external.sap.auth.dto.TokenResponse
import com.otoki.powersales.external.sap.auth.exception.SapInvalidClientException
import com.otoki.powersales.external.sap.auth.exception.SapIpNotAllowedException
import com.otoki.powersales.external.sap.auth.exception.SapUnsupportedGrantTypeException
import com.otoki.powersales.external.sap.auth.util.IpAllowlistMatcher
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

        // scope 검증 제거: 폐쇄망(private network) + IP allowlist + client_id/secret 인증으로
        // 신뢰 경계가 확보되어 scope 세분화를 적용하지 않는다. 요청 scope 값과 무관하게
        // 모든 inbound API 권한(allowedScopes 전체)을 토큰에 부여한다.
        val grantedScopes = properties.allowedScopes

        val issued = jwtCodec.issue(clientId = clientId, scopes = grantedScopes)
        auditService.record(
            SapInboundAudit(
                eventType = SapInboundAuditEventType.TOKEN_ISSUED,
                clientId = clientId,
                endpoint = TOKEN_ENDPOINT,
                httpMethod = "POST",
                clientIp = clientIp,
                scope = grantedScopes.joinToString(" "),
                reason = "jti=${issued.jti}"
            )
        )
        return TokenResponse(
            accessToken = issued.token,
            tokenType = "Bearer",
            expiresIn = issued.expiresIn,
            scope = grantedScopes.joinToString(" ")
        )
    }

    companion object {
        const val GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials"
        const val TOKEN_ENDPOINT = "/api/v1/sap/oauth/token"
    }
}
