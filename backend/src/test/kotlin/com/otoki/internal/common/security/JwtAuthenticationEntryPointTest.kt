package com.otoki.internal.common.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException

@DisplayName("JwtAuthenticationEntryPoint 테스트")
class JwtAuthenticationEntryPointTest {

    private val objectMapper = ObjectMapper().apply {
        registerModule(JavaTimeModule())
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    }
    private lateinit var entryPoint: JwtAuthenticationEntryPoint
    private lateinit var request: MockHttpServletRequest
    private lateinit var response: MockHttpServletResponse

    @BeforeEach
    fun setUp() {
        entryPoint = JwtAuthenticationEntryPoint(objectMapper)
        request = MockHttpServletRequest()
        response = MockHttpServletResponse()
    }

    @Nested
    @DisplayName("commence - 인증 실패 응답")
    inner class CommenceTests {

        @Test
        @DisplayName("만료된 토큰 - 401 + TOKEN_EXPIRED")
        fun commence_expiredToken_returnsTokenExpired() {
            // Given
            request.setAttribute("jwt.expired", true)

            // When
            entryPoint.commence(request, response, AuthenticationCredentialsNotFoundException("test"))

            // Then
            assertThat(response.status).isEqualTo(401)
            assertThat(response.contentType).isEqualTo("application/json;charset=UTF-8")

            val body = objectMapper.readTree(response.contentAsString)
            assertThat(body.get("success").asBoolean()).isFalse()
            assertThat(body.get("data").isNull).isTrue()
            assertThat(body.get("error").get("code").asText()).isEqualTo("TOKEN_EXPIRED")
            assertThat(body.get("error").get("message").asText()).isEqualTo("토큰이 만료되었습니다")
        }

        @Test
        @DisplayName("토큰 미제공 - 401 + UNAUTHORIZED")
        fun commence_noToken_returnsUnauthorized() {
            // Given: jwt.expired attribute 미설정

            // When
            entryPoint.commence(request, response, AuthenticationCredentialsNotFoundException("test"))

            // Then
            assertThat(response.status).isEqualTo(401)
            assertThat(response.contentType).isEqualTo("application/json;charset=UTF-8")

            val body = objectMapper.readTree(response.contentAsString)
            assertThat(body.get("success").asBoolean()).isFalse()
            assertThat(body.get("error").get("code").asText()).isEqualTo("UNAUTHORIZED")
            assertThat(body.get("error").get("message").asText()).isEqualTo("인증이 필요합니다")
        }

        @Test
        @DisplayName("잘못된 토큰 (jwt.expired=false) - 401 + UNAUTHORIZED")
        fun commence_invalidToken_returnsUnauthorized() {
            // Given: jwt.expired 미설정 (잘못된 서명 등)

            // When
            entryPoint.commence(request, response, AuthenticationCredentialsNotFoundException("test"))

            // Then
            assertThat(response.status).isEqualTo(401)
            val body = objectMapper.readTree(response.contentAsString)
            assertThat(body.get("error").get("code").asText()).isEqualTo("UNAUTHORIZED")
        }

        @Test
        @DisplayName("응답 body에 timestamp 포함")
        fun commence_responseContainsTimestamp() {
            // When
            entryPoint.commence(request, response, AuthenticationCredentialsNotFoundException("test"))

            // Then
            val body = objectMapper.readTree(response.contentAsString)
            assertThat(body.has("timestamp")).isTrue()
        }
    }
}
