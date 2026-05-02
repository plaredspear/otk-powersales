package com.otoki.powersales.sap.auth.filter

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.config.SapAuthProperties
import com.otoki.powersales.sap.auth.service.SapJwtCodec
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import tools.jackson.databind.ObjectMapper

@DisplayName("SapBearerTokenFilter 테스트")
class SapBearerTokenFilterTest {

    private val signingKey = "sap-bearer-filter-test-signing-key-with-256-bits-of-entropy-12"
    private val codec = SapJwtCodec(
        SapAuthProperties(jwtSigningKey = signingKey, tokenTtlSeconds = 86400)
    )
    private val auditService: SapInboundAuditService = mock<SapInboundAuditService>().apply {
        whenever(this.record(any())).thenAnswer { it.getArgument<SapInboundAudit>(0) }
    }
    private val objectMapper = ObjectMapper()
    private val filter = SapBearerTokenFilter(codec, auditService, objectMapper)
    private val chain: FilterChain = mock()

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Nested
    @DisplayName("Happy Path")
    inner class HappyPath {

        @Test
        @DisplayName("토큰 발급 경로 - 검증 건너뛰고 통과")
        fun tokenEndpoint_skip() {
            val request = MockHttpServletRequest("POST", "/api/v1/sap/oauth/token")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, chain)

            verify(chain).doFilter(request, response)
            assertThat(SecurityContextHolder.getContext().authentication).isNull()
        }

        @Test
        @DisplayName("유효 토큰 - SecurityContext 에 SCOPE_* authority 주입")
        fun validToken_setsAuthorities() {
            val issued = codec.issue("client-1", listOf("sap.org.write", "sap.employee.write"))
            val request = MockHttpServletRequest("POST", "/api/v1/sap/org-master").apply {
                addHeader("Authorization", "Bearer ${issued.token}")
            }
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, chain)

            val auth = SecurityContextHolder.getContext().authentication
            assertThat(auth).isNotNull
            assertThat(auth!!.principal).isEqualTo("client-1")
            assertThat(auth.authorities.map { it.authority })
                .containsExactlyInAnyOrder("SCOPE_sap.org.write", "SCOPE_sap.employee.write")
            verify(chain).doFilter(request, response)
        }
    }

    @Nested
    @DisplayName("Error Path")
    inner class ErrorPath {

        @Test
        @DisplayName("Authorization 헤더 누락 -> 401 INVALID_TOKEN")
        fun missingHeader_unauthorized() {
            val request = MockHttpServletRequest("POST", "/api/v1/sap/org-master")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, chain)

            assertThat(response.status).isEqualTo(401)
            assertThat(response.contentAsString).contains("INVALID_TOKEN")
        }

        @Test
        @DisplayName("Bearer prefix 없음 -> 401")
        fun noBearerPrefix_unauthorized() {
            val request = MockHttpServletRequest("POST", "/api/v1/sap/org-master").apply {
                addHeader("Authorization", "Token abc")
            }
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, chain)

            assertThat(response.status).isEqualTo(401)
        }

        @Test
        @DisplayName("서명 변조된 토큰 -> 401")
        fun tampered_unauthorized() {
            val issued = codec.issue("client-1", listOf("sap.org.write"))
            val tampered = issued.token.dropLast(2) + "AB"
            val request = MockHttpServletRequest("POST", "/api/v1/sap/org-master").apply {
                addHeader("Authorization", "Bearer $tampered")
            }
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, chain)

            assertThat(response.status).isEqualTo(401)
        }
    }
}
