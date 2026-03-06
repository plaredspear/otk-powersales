package com.otoki.internal.sap.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.otoki.internal.sap.config.SapAuthProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

@DisplayName("SapApiKeyFilter 테스트")
class SapApiKeyFilterTest {

    private val objectMapper = ObjectMapper().apply {
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    }
    private val validApiKey = "test-sap-api-key-12345"

    @Nested
    @DisplayName("API Key 인증")
    inner class ApiKeyAuthTests {

        private val filter = SapApiKeyFilter(
            SapAuthProperties(apiKey = validApiKey, allowedIps = ""),
            objectMapper
        )

        @Test
        @DisplayName("유효한 API Key - 인증 통과")
        fun validApiKey_passes() {
            val request = MockHttpServletRequest().apply {
                addHeader("X-API-Key", validApiKey)
            }
            val response = MockHttpServletResponse()
            val filterChain = MockFilterChain()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(200)
            assertThat(filterChain.request).isNotNull
        }

        @Test
        @DisplayName("API Key 누락 - 401 응답")
        fun missingApiKey_returns401() {
            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()
            val filterChain = MockFilterChain()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(401)
            assertThat(response.contentAsString).contains("INVALID_API_KEY")
            assertThat(filterChain.request).isNull()
        }

        @Test
        @DisplayName("빈 API Key - 401 응답")
        fun emptyApiKey_returns401() {
            val request = MockHttpServletRequest().apply {
                addHeader("X-API-Key", "")
            }
            val response = MockHttpServletResponse()
            val filterChain = MockFilterChain()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(401)
            assertThat(response.contentAsString).contains("INVALID_API_KEY")
        }

        @Test
        @DisplayName("잘못된 API Key - 401 응답")
        fun wrongApiKey_returns401() {
            val request = MockHttpServletRequest().apply {
                addHeader("X-API-Key", "wrong-key")
            }
            val response = MockHttpServletResponse()
            val filterChain = MockFilterChain()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(401)
            assertThat(response.contentAsString).contains("INVALID_API_KEY")
        }

        @Test
        @DisplayName("인증 성공 시 SecurityContext에 SAP 인증 정보 설정")
        fun validApiKey_setsSapAuthentication() {
            val request = MockHttpServletRequest().apply {
                addHeader("X-API-Key", validApiKey)
            }
            val response = MockHttpServletResponse()
            val filterChain = MockFilterChain()

            filter.doFilter(request, response, filterChain)

            val auth = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication
            assertThat(auth).isNotNull
            assertThat(auth.principal).isEqualTo("SAP_SYSTEM")
            assertThat(auth.authorities.map { it.authority }).contains("ROLE_SAP")
        }
    }

    @Nested
    @DisplayName("IP 화이트리스트")
    inner class IpWhitelistTests {

        private val filter = SapApiKeyFilter(
            SapAuthProperties(apiKey = validApiKey, allowedIps = "10.0.1.100,10.0.1.101"),
            objectMapper
        )

        @Test
        @DisplayName("허용된 IP - 인증 통과")
        fun allowedIp_passes() {
            val request = MockHttpServletRequest().apply {
                addHeader("X-API-Key", validApiKey)
                remoteAddr = "10.0.1.100"
            }
            val response = MockHttpServletResponse()
            val filterChain = MockFilterChain()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(200)
            assertThat(filterChain.request).isNotNull
        }

        @Test
        @DisplayName("허용되지 않은 IP - 403 응답")
        fun blockedIp_returns403() {
            val request = MockHttpServletRequest().apply {
                addHeader("X-API-Key", validApiKey)
                remoteAddr = "192.168.1.1"
            }
            val response = MockHttpServletResponse()
            val filterChain = MockFilterChain()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(403)
            assertThat(response.contentAsString).contains("ACCESS_DENIED")
            assertThat(filterChain.request).isNull()
        }

        @Test
        @DisplayName("API Key 틀리면 IP 검증 전에 401 반환")
        fun wrongApiKey_returns401BeforeIpCheck() {
            val request = MockHttpServletRequest().apply {
                addHeader("X-API-Key", "wrong-key")
                remoteAddr = "10.0.1.100"
            }
            val response = MockHttpServletResponse()
            val filterChain = MockFilterChain()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(401)
        }
    }

    @Nested
    @DisplayName("IP 화이트리스트 미설정 (빈값)")
    inner class NoIpWhitelistTests {

        private val filter = SapApiKeyFilter(
            SapAuthProperties(apiKey = validApiKey, allowedIps = ""),
            objectMapper
        )

        @Test
        @DisplayName("IP 목록 빈값 - 모든 IP 허용")
        fun emptyAllowedIps_allowsAll() {
            val request = MockHttpServletRequest().apply {
                addHeader("X-API-Key", validApiKey)
                remoteAddr = "192.168.1.1"
            }
            val response = MockHttpServletResponse()
            val filterChain = MockFilterChain()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(200)
            assertThat(filterChain.request).isNotNull
        }
    }
}
