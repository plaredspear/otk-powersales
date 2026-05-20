package com.otoki.powersales.sf.auth.service

import com.otoki.powersales.sf.auth.audit.SfInboundAudit
import com.otoki.powersales.sf.auth.audit.SfInboundAuditEventType
import com.otoki.powersales.sf.auth.audit.SfInboundAuditService
import com.otoki.powersales.sf.auth.config.SfAuthProperties
import com.otoki.powersales.sf.auth.dto.TokenRequest
import com.otoki.powersales.sf.auth.exception.SfInvalidClientException
import com.otoki.powersales.sf.auth.exception.SfInvalidScopeException
import com.otoki.powersales.sf.auth.exception.SfUnsupportedGrantTypeException
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

@DisplayName("SfTokenService 테스트")
class SfTokenServiceTest {

    private val plainSecret = "super-secret-pw"
    private val secretHash: String = BCryptPasswordEncoder().encode(plainSecret)!!
    private val signingKey = "sf-token-service-test-signing-key-with-256-bits-1234567890"

    private val auditService: SfInboundAuditService = mockk()

    private fun service(
        allowedScopes: List<String> = listOf("sf.write")
    ): SfTokenService {
        val props = SfAuthProperties(
            clientId = "otoki-sf-client",
            clientSecretHash = secretHash,
            jwtSigningKey = signingKey,
            tokenTtlSeconds = 86400,
            allowedScopes = allowedScopes
        )
        every { auditService.record(any()) } answers { firstArg() }
        return SfTokenService(props, auditService, SfJwtCodec(props))
    }

    private fun validRequest(scope: String = "sf.write"): TokenRequest {
        return TokenRequest(
            grantType = "client_credentials",
            clientId = "otoki-sf-client",
            clientSecret = plainSecret,
            scope = scope
        )
    }

    @Nested
    @DisplayName("issue - Happy Path")
    inner class HappyPath {

        @Test
        @DisplayName("정상 발급 - JWT + expires_in=86400 + scope=sf.write 반환")
        fun issue_success() {
            val auditSlot = slot<SfInboundAudit>()
            val svc = service()
            every { auditService.record(capture(auditSlot)) } answers { firstArg() }

            val response = svc.issue(validRequest(), "127.0.0.1")
            assertThat(response.accessToken).isNotBlank()
            assertThat(response.tokenType).isEqualTo("Bearer")
            assertThat(response.expiresIn).isEqualTo(86400)
            assertThat(response.scope).isEqualTo("sf.write")

            verify { auditService.record(any()) }
            assertThat(auditSlot.captured.eventType).isEqualTo(SfInboundAuditEventType.TOKEN_ISSUED)
            assertThat(auditSlot.captured.clientId).isEqualTo("otoki-sf-client")
        }
    }

    @Nested
    @DisplayName("issue - Error Path")
    inner class ErrorPath {

        @Test
        @DisplayName("client_secret 불일치 -> SfInvalidClientException + TOKEN_REJECTED 적재")
        fun issue_invalidSecret() {
            val req = validRequest().copy(clientSecret = "wrong-secret")
            val auditSlot = slot<SfInboundAudit>()
            val svc = service()
            every { auditService.record(capture(auditSlot)) } answers { firstArg() }

            assertThatThrownBy { svc.issue(req, "127.0.0.1") }
                .isInstanceOf(SfInvalidClientException::class.java)

            verify { auditService.record(any()) }
            assertThat(auditSlot.captured.eventType).isEqualTo(SfInboundAuditEventType.TOKEN_REJECTED)
        }

        @Test
        @DisplayName("client_id 불일치 -> SfInvalidClientException")
        fun issue_invalidClientId() {
            val req = validRequest().copy(clientId = "unknown-client")
            assertThatThrownBy { service().issue(req, "127.0.0.1") }
                .isInstanceOf(SfInvalidClientException::class.java)
        }

        @Test
        @DisplayName("grant_type 미지원 -> SfUnsupportedGrantTypeException")
        fun issue_unsupportedGrant() {
            val req = validRequest().copy(grantType = "password")
            assertThatThrownBy { service().issue(req, "127.0.0.1") }
                .isInstanceOf(SfUnsupportedGrantTypeException::class.java)
        }

        @Test
        @DisplayName("미허용 scope 요청 -> SfInvalidScopeException")
        fun issue_invalidScope() {
            val req = validRequest(scope = "sap.write")
            assertThatThrownBy { service().issue(req, "127.0.0.1") }
                .isInstanceOf(SfInvalidScopeException::class.java)
        }

        @Test
        @DisplayName("scope 누락(빈 문자열) -> SfInvalidScopeException")
        fun issue_emptyScope() {
            val req = validRequest(scope = "")
            assertThatThrownBy { service().issue(req, "127.0.0.1") }
                .isInstanceOf(SfInvalidScopeException::class.java)
        }
    }
}
