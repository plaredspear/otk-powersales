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
    private val sapDomain = "dev-pwrs-sap.codapt.kr"
    private val filter = DomainGuardFilter(apiDomain, adminDomain, sapDomain)
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
        @DisplayName("API 도메인 + OpenAPI 스펙 경로 - 통과")
        fun apiDomain_openApiDocsPath_passes() {
            val request = createRequest(apiDomain, "/v3/api-docs/swagger-config")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
        }

        @Test
        @DisplayName("API 도메인 + H2 콘솔 경로 - 통과")
        fun apiDomain_h2ConsolePath_passes() {
            val request = createRequest(apiDomain, "/h2-console/login.do")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
        }

        @Test
        @DisplayName("API 도메인 + SPA 라우트 - 404 차단")
        fun apiDomain_spaRoute_blocked() {
            val request = createRequest(apiDomain, "/sales/monthly")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(404)
            verifyNoInteractions(filterChain)
        }

        @Test
        @DisplayName("API 도메인 + 루트 경로 - 404 차단")
        fun apiDomain_rootPath_blocked() {
            val request = createRequest(apiDomain, "/")
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
        @DisplayName("Admin 도메인 + 인증 API 허용 - 통과")
        fun adminDomain_authApi_passes() {
            val request = createRequest(adminDomain, "/api/v1/auth/login")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
        }

        @Test
        @DisplayName("Admin 도메인 + 인증 갱신 API 허용 - 통과")
        fun adminDomain_authRefreshApi_passes() {
            val request = createRequest(adminDomain, "/api/v1/auth/refresh")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
        }

        @Test
        @DisplayName("Admin 도메인 + 관리자 API 허용 - 통과")
        fun adminDomain_adminApi_passes() {
            val request = createRequest(adminDomain, "/api/v1/admin/dashboard")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
        }

        @Test
        @DisplayName("Admin 도메인 + 모바일 API 차단 - 404")
        fun adminDomain_mobileApi_blocked() {
            val request = createRequest(adminDomain, "/api/v1/home/dashboard")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(404)
            verifyNoInteractions(filterChain)
        }

        @Test
        @DisplayName("Admin 도메인 + SPA 라우트 - 통과")
        fun adminDomain_spaRoute_passes() {
            val request = createRequest(adminDomain, "/sales/monthly")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
        }

        @Test
        @DisplayName("Admin 도메인 + 루트 경로 - 통과")
        fun adminDomain_rootPath_passes() {
            val request = createRequest(adminDomain, "/")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
        }
    }

    @Nested
    @DisplayName("SAP 도메인 요청")
    inner class SapDomainTests {

        @Test
        @DisplayName("SAP 도메인 + SAP API 경로 - 통과")
        fun sapDomain_sapApiPath_passes() {
            val request = createRequest(sapDomain, "/api/v1/sap/organize-master")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
        }

        @Test
        @DisplayName("SAP 도메인 + 비SAP API 경로 - 404 차단")
        fun sapDomain_nonSapApiPath_blocked() {
            val request = createRequest(sapDomain, "/api/v1/health")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(404)
            verifyNoInteractions(filterChain)
        }

        @Test
        @DisplayName("SAP 도메인 미설정 시 API 도메인에서 SAP 경로 허용")
        fun noSapDomain_apiDomainAllowsSapPath() {
            val filterNoSap = DomainGuardFilter(apiDomain, adminDomain, "")
            val request = createRequest(apiDomain, "/api/v1/sap/organize-master")
            val response = MockHttpServletResponse()

            filterNoSap.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
        }
    }

    @Nested
    @DisplayName("미인식 도메인 요청")
    inner class UnrecognizedDomainTests {

        @Test
        @DisplayName("localhost - 모든 경로 통과")
        fun localhost_allPaths_pass() {
            val request = createRequest("localhost", "/sales/monthly")
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
            val emptyFilter = DomainGuardFilter("", "", "")
            val request = createRequest("any-host.com", "/sales/monthly")
            val response = MockHttpServletResponse()

            emptyFilter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
        }
    }

    @Nested
    @DisplayName("Host 헤더 포트 처리")
    inner class HostPortTests {

        @Test
        @DisplayName("Host에 포트 포함 시 포트 제거 후 매칭 - SPA 라우트 차단")
        fun hostWithPort_stripsPort() {
            val request = createRequest("$apiDomain:443", "/sales/monthly")
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
