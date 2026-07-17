package com.otoki.powersales.external.rdp.service

import com.otoki.powersales.external.rdp.auth.audit.RdpInboundAudit
import com.otoki.powersales.external.rdp.auth.audit.RdpInboundAuditEventType
import com.otoki.powersales.external.rdp.auth.audit.RdpInboundAuditService
import com.otoki.powersales.external.rdp.auth.config.RdpAuthProperties
import com.otoki.powersales.external.rdp.auth.dto.TokenRequest
import com.otoki.powersales.external.rdp.auth.exception.RdpInvalidClientException
import com.otoki.powersales.external.rdp.auth.exception.RdpInvalidScopeException
import com.otoki.powersales.external.rdp.auth.exception.RdpUnsupportedGrantTypeException
import com.otoki.powersales.external.rdp.auth.service.RdpJwtCodec
import com.otoki.powersales.external.rdp.auth.service.RdpTokenService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@DisplayName("RdpTokenService 테스트")
class RdpTokenServiceTest {

    private val plainSecret = "super-secret-pw"
    private val secretHash: String = BCryptPasswordEncoder().encode(plainSecret)!!
    private val signingKey = "rdp-token-service-test-signing-key-with-256-bits-1234567890"

    private val auditService: RdpInboundAuditService = mockk()

    private fun service(
        allowedScopes: List<String> = listOf("rdp.write")
    ): RdpTokenService {
        val props = RdpAuthProperties(
            clientId = "otoki-rdp-client",
            clientSecretHash = secretHash,
            jwtSigningKey = signingKey,
            tokenTtlSeconds = 86400,
            allowedScopes = allowedScopes
        )
        every { auditService.record(any()) } answers { firstArg() }
        return RdpTokenService(props, auditService, RdpJwtCodec(props))
    }

    private fun validRequest(scope: String = "rdp.write"): TokenRequest {
        return TokenRequest(
            grantType = "client_credentials",
            clientId = "otoki-rdp-client",
            clientSecret = plainSecret,
            scope = scope
        )
    }

    @Nested
    @DisplayName("issue - Happy Path")
    inner class HappyPath {

        @Test
        @DisplayName("정상 발급 - JWT + expires_in=86400 + scope=rdp.write 반환")
        fun issue_success() {
            val auditSlot = slot<RdpInboundAudit>()
            val svc = service()
            every { auditService.record(capture(auditSlot)) } answers { firstArg() }

            val response = svc.issue(validRequest(), "127.0.0.1")
            assertThat(response.accessToken).isNotBlank()
            assertThat(response.tokenType).isEqualTo("Bearer")
            assertThat(response.expiresIn).isEqualTo(86400)
            assertThat(response.scope).isEqualTo("rdp.write")

            verify { auditService.record(any()) }
            assertThat(auditSlot.captured.eventType).isEqualTo(RdpInboundAuditEventType.TOKEN_ISSUED)
            assertThat(auditSlot.captured.clientId).isEqualTo("otoki-rdp-client")
        }
    }

    @Nested
    @DisplayName("issue - Error Path")
    inner class ErrorPath {

        @Test
        @DisplayName("client_secret 불일치 -> RdpInvalidClientException + TOKEN_REJECTED 적재")
        fun issue_invalidSecret() {
            val req = validRequest().copy(clientSecret = "wrong-secret")
            val auditSlot = slot<RdpInboundAudit>()
            val svc = service()
            every { auditService.record(capture(auditSlot)) } answers { firstArg() }

            assertThatThrownBy { svc.issue(req, "127.0.0.1") }
                .isInstanceOf(RdpInvalidClientException::class.java)

            verify { auditService.record(any()) }
            assertThat(auditSlot.captured.eventType).isEqualTo(RdpInboundAuditEventType.TOKEN_REJECTED)
        }

        @Test
        @DisplayName("client_id 불일치 -> RdpInvalidClientException")
        fun issue_invalidClientId() {
            val req = validRequest().copy(clientId = "unknown-client")
            assertThatThrownBy { service().issue(req, "127.0.0.1") }
                .isInstanceOf(RdpInvalidClientException::class.java)
        }

        @Test
        @DisplayName("grant_type 미지원 -> RdpUnsupportedGrantTypeException")
        fun issue_unsupportedGrant() {
            val req = validRequest().copy(grantType = "password")
            assertThatThrownBy { service().issue(req, "127.0.0.1") }
                .isInstanceOf(RdpUnsupportedGrantTypeException::class.java)
        }

        @Test
        @DisplayName("미허용 scope 요청 -> RdpInvalidScopeException")
        fun issue_invalidScope() {
            val req = validRequest(scope = "sap.write")
            assertThatThrownBy { service().issue(req, "127.0.0.1") }
                .isInstanceOf(RdpInvalidScopeException::class.java)
        }

        @Test
        @DisplayName("scope 누락(빈 문자열) -> RdpInvalidScopeException")
        fun issue_emptyScope() {
            val req = validRequest(scope = "")
            assertThatThrownBy { service().issue(req, "127.0.0.1") }
                .isInstanceOf(RdpInvalidScopeException::class.java)
        }
    }
}
