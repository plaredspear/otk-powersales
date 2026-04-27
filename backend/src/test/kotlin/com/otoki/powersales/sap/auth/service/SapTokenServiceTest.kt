package com.otoki.powersales.sap.auth.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.config.SapAuthProperties
import com.otoki.powersales.sap.auth.dto.TokenRequest
import com.otoki.powersales.sap.auth.exception.SapInvalidClientException
import com.otoki.powersales.sap.auth.exception.SapInvalidScopeException
import com.otoki.powersales.sap.auth.exception.SapIpNotAllowedException
import com.otoki.powersales.sap.auth.exception.SapUnsupportedGrantTypeException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@DisplayName("SapTokenService 테스트")
class SapTokenServiceTest {

    private val plainSecret = "super-secret-pw"
    private val secretHash: String = BCryptPasswordEncoder().encode(plainSecret)!!
    private val signingKey = "sap-token-service-test-signing-key-with-256-bits-1234567890"

    private val auditService: SapInboundAuditService = mock()

    private fun service(
        allowedIps: List<String> = emptyList(),
        allowedScopes: List<String> = listOf(
            "sap.org.write", "sap.employee.write", "sap.account.write"
        )
    ): SapTokenService {
        val props = SapAuthProperties(
            clientId = "otoki-sap-client",
            clientSecretHash = secretHash,
            jwtSigningKey = signingKey,
            tokenTtlSeconds = 900,
            allowedScopes = allowedScopes,
            allowedIps = allowedIps
        )
        whenever(auditService.record(any())).thenAnswer { it.getArgument<SapInboundAudit>(0) }
        return SapTokenService(props, auditService, SapJwtCodec(props))
    }

    private fun validRequest(scope: String = "sap.org.write sap.employee.write"): TokenRequest {
        return TokenRequest(
            grantType = "client_credentials",
            clientId = "otoki-sap-client",
            clientSecret = plainSecret,
            scope = scope
        )
    }

    @Nested
    @DisplayName("issue - Happy Path")
    inner class HappyPath {

        @Test
        @DisplayName("정상 발급 - JWT + expires_in=900 + scope 반환")
        fun issue_success() {
            val response = service().issue(validRequest(), "127.0.0.1")
            assertThat(response.accessToken).isNotBlank()
            assertThat(response.tokenType).isEqualTo("Bearer")
            assertThat(response.expiresIn).isEqualTo(900)
            assertThat(response.scope).isEqualTo("sap.org.write sap.employee.write")

            val captor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(captor.capture())
            assertThat(captor.firstValue.eventType).isEqualTo(SapInboundAuditEventType.TOKEN_ISSUED)
            assertThat(captor.firstValue.clientId).isEqualTo("otoki-sap-client")
        }
    }

    @Nested
    @DisplayName("issue - Error Path")
    inner class ErrorPath {

        @Test
        @DisplayName("client_secret 불일치 -> SapInvalidClientException + TOKEN_REJECTED 적재")
        fun issue_invalidSecret() {
            val req = validRequest().copy(clientSecret = "wrong-secret")
            val svc = service()
            assertThatThrownBy { svc.issue(req, "127.0.0.1") }
                .isInstanceOf(SapInvalidClientException::class.java)

            val captor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(captor.capture())
            assertThat(captor.firstValue.eventType).isEqualTo(SapInboundAuditEventType.TOKEN_REJECTED)
        }

        @Test
        @DisplayName("client_id 불일치 -> SapInvalidClientException")
        fun issue_invalidClientId() {
            val req = validRequest().copy(clientId = "unknown-client")
            assertThatThrownBy { service().issue(req, "127.0.0.1") }
                .isInstanceOf(SapInvalidClientException::class.java)
        }

        @Test
        @DisplayName("grant_type 미지원 -> SapUnsupportedGrantTypeException")
        fun issue_unsupportedGrant() {
            val req = validRequest().copy(grantType = "password")
            assertThatThrownBy { service().issue(req, "127.0.0.1") }
                .isInstanceOf(SapUnsupportedGrantTypeException::class.java)
        }

        @Test
        @DisplayName("미허용 scope 요청 -> SapInvalidScopeException")
        fun issue_invalidScope() {
            val req = validRequest(scope = "sap.unknown.write")
            assertThatThrownBy { service().issue(req, "127.0.0.1") }
                .isInstanceOf(SapInvalidScopeException::class.java)
        }

        @Test
        @DisplayName("scope 누락(빈 문자열) -> SapInvalidScopeException")
        fun issue_emptyScope() {
            val req = validRequest(scope = "")
            assertThatThrownBy { service().issue(req, "127.0.0.1") }
                .isInstanceOf(SapInvalidScopeException::class.java)
        }

        @Test
        @DisplayName("IP allowlist 차단 - 허용 IP 외 호출 -> SapIpNotAllowedException + REQUEST_REJECTED_IP 적재")
        fun issue_ipBlocked() {
            val svc = service(allowedIps = listOf("10.0.0.0/8"))
            assertThatThrownBy { svc.issue(validRequest(), "203.0.113.5") }
                .isInstanceOf(SapIpNotAllowedException::class.java)

            val captor = argumentCaptor<SapInboundAudit>()
            verify(auditService).record(captor.capture())
            assertThat(captor.firstValue.eventType)
                .isEqualTo(SapInboundAuditEventType.REQUEST_REJECTED_IP)
        }

        @Test
        @DisplayName("IP allowlist 통과 - 허용 IP 내 호출 -> 정상 발급")
        fun issue_ipAllowed() {
            val svc = service(allowedIps = listOf("10.0.0.0/8"))
            val response = svc.issue(validRequest(), "10.1.2.3")
            assertThat(response.accessToken).isNotBlank()
        }
    }
}
