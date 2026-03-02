package com.otoki.internal.common.security

import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

@DisplayName("DomainGuardFilter 테스트")
class DomainGuardFilterTest {

    private val apiDomain = "dev-pwrs-api.codapt.kr"
    private val adminDomain = "dev-pwrs-admin.codapt.kr"
    private val filter = DomainGuardFilter(apiDomain, adminDomain)
    private val filterChain = mock(FilterChain::class.java)

    @Nested
    @DisplayName("API 도메인 요청")
    inner class ApiDomainTests {

        @Test
        @DisplayName("API 도메인 + API 경로 - 통과")
        fun apiDomain_apiPath_passes() {
            val request = createRequest(apiDomain, "/api/v1/health")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
        }

        @Test
        @DisplayName("API 도메인 + Swagger 경로 - 통과")
        fun apiDomain_swaggerPath_passes() {
            val request = createRequest(apiDomain, "/swagger-ui/index.html")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
        }

        @Test
        @DisplayName("API 도메인 + Admin 경로 - 404 차단")
        fun apiDomain_adminPath_blocked() {
            val request = createRequest(apiDomain, "/admin/login")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(404)
            verifyNoInteractions(filterChain)
        }
    }

    @Nested
    @DisplayName("Admin 도메인 요청")
    inner class AdminDomainTests {

        @Test
        @DisplayName("Admin 도메인 + Admin 경로 - 통과")
        fun adminDomain_adminPath_passes() {
            val request = createRequest(adminDomain, "/admin/dashboard")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
        }

        @Test
        @DisplayName("Admin 도메인 + API 경로 - 404 차단")
        fun adminDomain_apiPath_blocked() {
            val request = createRequest(adminDomain, "/api/v1/health")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(404)
            verifyNoInteractions(filterChain)
        }
    }

    @Nested
    @DisplayName("미인식 도메인 요청")
    inner class UnrecognizedDomainTests {

        @Test
        @DisplayName("localhost - 모든 경로 통과")
        fun localhost_allPaths_pass() {
            val request = createRequest("localhost", "/admin/login")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
        }

        @Test
        @DisplayName("IP 주소 (ALB health check) - 통과")
        fun ipAddress_passes() {
            val request = createRequest("10.0.1.50", "/api/v1/health")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
        }
    }

    @Nested
    @DisplayName("도메인 미설정 (빈 문자열)")
    inner class EmptyDomainTests {

        @Test
        @DisplayName("도메인 미설정 시 모든 경로 통과")
        fun emptyDomain_allPaths_pass() {
            val emptyFilter = DomainGuardFilter("", "")
            val request = createRequest("any-host.com", "/admin/login")
            val response = MockHttpServletResponse()

            emptyFilter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
        }
    }

    @Nested
    @DisplayName("Host 헤더 포트 처리")
    inner class HostPortTests {

        @Test
        @DisplayName("Host에 포트 포함 시 포트 제거 후 매칭 - 404 차단")
        fun hostWithPort_stripsPort() {
            val request = createRequest("$apiDomain:443", "/admin/login")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(404)
            verifyNoInteractions(filterChain)
        }
    }

    private fun createRequest(host: String, path: String): MockHttpServletRequest {
        val request = MockHttpServletRequest()
        request.addHeader("Host", host)
        request.requestURI = path
        return request
    }
}
