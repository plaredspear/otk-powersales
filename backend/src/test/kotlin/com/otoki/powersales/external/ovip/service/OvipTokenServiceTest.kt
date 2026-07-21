package com.otoki.powersales.external.ovip.service

import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAudit
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAuditEventType
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAuditService
import com.otoki.powersales.external.ovip.auth.config.OvipAuthProperties
import com.otoki.powersales.external.ovip.auth.dto.TokenRequest
import com.otoki.powersales.external.ovip.auth.exception.OvipInvalidClientException
import com.otoki.powersales.external.ovip.auth.exception.OvipUnsupportedGrantTypeException
import com.otoki.powersales.external.ovip.auth.service.OvipJwtCodec
import com.otoki.powersales.external.ovip.auth.service.OvipTokenService
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

@DisplayName("OvipTokenService 테스트")
class OvipTokenServiceTest {

    private val plainSecret = "super-secret-pw"
    private val secretHash: String = BCryptPasswordEncoder().encode(plainSecret)!!
    private val signingKey = "ovip-token-service-test-signing-key-with-256-bits-1234567890"

    private val auditService: OvipInboundAuditService = mockk()

    private fun service(): OvipTokenService {
        val props = OvipAuthProperties(
            clientId = "otoki-ovip-client",
            clientSecretHash = secretHash,
            jwtSigningKey = signingKey,
            tokenTtlSeconds = 86400
        )
        every { auditService.record(any()) } answers { firstArg() }
        return OvipTokenService(props, auditService, OvipJwtCodec(props))
    }

    private fun validRequest(scope: String = "ovip.write"): TokenRequest {
        return TokenRequest(
            grantType = "client_credentials",
            clientId = "otoki-ovip-client",
            clientSecret = plainSecret,
            scope = scope
        )
    }

    @Nested
    @DisplayName("issue - Happy Path")
    inner class HappyPath {

        @Test
        @DisplayName("정상 발급 - JWT + expires_in=86400 + scope=ovip.write 반환")
        fun issue_success() {
            val auditSlot = slot<OvipInboundAudit>()
            val svc = service()
            every { auditService.record(capture(auditSlot)) } answers { firstArg() }

            val response = svc.issue(validRequest(), "127.0.0.1")
            assertThat(response.accessToken).isNotBlank()
            assertThat(response.tokenType).isEqualTo("Bearer")
            assertThat(response.expiresIn).isEqualTo(86400)
            assertThat(response.scope).isEqualTo("ovip.write")

            verify { auditService.record(any()) }
            assertThat(auditSlot.captured.eventType).isEqualTo(OvipInboundAuditEventType.TOKEN_ISSUED)
            assertThat(auditSlot.captured.clientId).isEqualTo("otoki-ovip-client")
        }
    }

    @Nested
    @DisplayName("issue - Error Path")
    inner class ErrorPath {

        @Test
        @DisplayName("client_secret 불일치 -> OvipInvalidClientException + TOKEN_REJECTED 적재")
        fun issue_invalidSecret() {
            val req = validRequest().copy(clientSecret = "wrong-secret")
            val auditSlot = slot<OvipInboundAudit>()
            val svc = service()
            every { auditService.record(capture(auditSlot)) } answers { firstArg() }

            assertThatThrownBy { svc.issue(req, "127.0.0.1") }
                .isInstanceOf(OvipInvalidClientException::class.java)

            verify { auditService.record(any()) }
            assertThat(auditSlot.captured.eventType).isEqualTo(OvipInboundAuditEventType.TOKEN_REJECTED)
        }

        @Test
        @DisplayName("client_id 불일치 -> OvipInvalidClientException")
        fun issue_invalidClientId() {
            val req = validRequest().copy(clientId = "unknown-client")
            assertThatThrownBy { service().issue(req, "127.0.0.1") }
                .isInstanceOf(OvipInvalidClientException::class.java)
        }

        @Test
        @DisplayName("grant_type 미지원 -> OvipUnsupportedGrantTypeException")
        fun issue_unsupportedGrant() {
            val req = validRequest().copy(grantType = "password")
            assertThatThrownBy { service().issue(req, "127.0.0.1") }
                .isInstanceOf(OvipUnsupportedGrantTypeException::class.java)
        }

    }

    @Nested
    @DisplayName("issue - scope 검증 없음 (권한 체크 미수행)")
    inner class NoScopeCheck {

        @Test
        @DisplayName("임의 scope 요청도 거부 없이 그대로 발급된다")
        fun issue_arbitraryScopeAccepted() {
            val response = service().issue(validRequest(scope = "sap.write"), "127.0.0.1")
            assertThat(response.accessToken).isNotBlank()
            assertThat(response.scope).isEqualTo("sap.write")
        }

        @Test
        @DisplayName("scope 누락(빈 문자열)이어도 빈 scope 로 발급된다")
        fun issue_emptyScopeAccepted() {
            val response = service().issue(validRequest(scope = ""), "127.0.0.1")
            assertThat(response.accessToken).isNotBlank()
            assertThat(response.scope).isEqualTo("")
        }
    }
}
