package com.otoki.powersales.platform.common.security

import com.otoki.powersales.platform.common.security.DomainGuardFilter
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

@DisplayName("DomainGuardFilter 테스트")
class DomainGuardFilterTest {

    private val apiDomain = "dev-powersalesapi.otoki.com"
    private val adminDomain = "dev-pwrs-admin.codapt.kr"
    private val filter = DomainGuardFilter(apiDomain, adminDomain)
    private val filterChain: FilterChain = mockk(relaxUnitFun = true)

    @Nested
    @DisplayName("API 도메인 (모바일 전용) 요청")
    inner class ApiDomainTests {

        @Test
        @DisplayName("API 도메인 + 모바일 인증 경로 - 통과")
        fun apiDomain_authPath_passes() {
            val request = createRequest(apiDomain, "/api/v1/mobile/auth/login")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify { filterChain.doFilter(request, response) }
        }

        @Test
        @DisplayName("API 도메인 + 모바일 데이터 경로 - 통과")
        fun apiDomain_salesPath_passes() {
            val request = createRequest(apiDomain, "/api/v1/mobile/sales/monthly")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify { filterChain.doFilter(request, response) }
        }

        @Test
        @DisplayName("API 도메인 + 헬스체크 경로 - 통과")
        fun apiDomain_healthPath_passes() {
            val request = createRequest(apiDomain, "/api/health")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify { filterChain.doFilter(request, response) }
        }

        @Test
        @DisplayName("API 도메인 + 구 모바일 prefix 경로(/mobile/ 미부착) - 404 차단")
        fun apiDomain_legacyMobilePath_blocked() {
            val request = createRequest(apiDomain, "/api/v1/home")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(404)
            verify { filterChain wasNot Called }
        }

        @Test
        @DisplayName("API 도메인 + Swagger 경로 - 통과")
        fun apiDomain_swaggerPath_passes() {
            val request = createRequest(apiDomain, "/swagger-ui/index.html")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify { filterChain.doFilter(request, response) }
        }

        @Test
        @DisplayName("API 도메인 + OpenAPI 스펙 경로 - 통과")
        fun apiDomain_openApiDocsPath_passes() {
            val request = createRequest(apiDomain, "/v3/api-docs/swagger-config")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify { filterChain.doFilter(request, response) }
        }

        @Test
        @DisplayName("API 도메인 + 관리자 API - 404 차단 (모바일 전용 도메인)")
        fun apiDomain_adminApi_blocked() {
            val request = createRequest(apiDomain, "/api/v1/admin/dashboard")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(404)
            verify { filterChain wasNot Called }
        }

        @Test
        @DisplayName("API 도메인 + 폐지된 SAP 경로 - 404 차단")
        fun apiDomain_legacySapPath_blocked() {
            val request = createRequest(apiDomain, "/api/v1/sap/account")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(404)
            verify { filterChain wasNot Called }
        }

        @Test
        @DisplayName("API 도메인 + SPA 라우트 - 404 차단")
        fun apiDomain_spaRoute_blocked() {
            val request = createRequest(apiDomain, "/sales/monthly")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(404)
            verify { filterChain wasNot Called }
        }

        @Test
        @DisplayName("API 도메인 + 루트 경로 - 404 차단")
        fun apiDomain_rootPath_blocked() {
            val request = createRequest(apiDomain, "/")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(404)
            verify { filterChain wasNot Called }
        }
    }

    @Nested
    @DisplayName("Admin 도메인 요청")
    inner class AdminDomainTests {

        @Test
        @DisplayName("Admin 도메인 + 모바일 인증 API 차단 - 404 (admin auth는 /api/v1/admin/auth/* 사용)")
        fun adminDomain_mobileAuthApi_blocked() {
            val request = createRequest(adminDomain, "/api/v1/mobile/auth/login")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(404)
            verify { filterChain wasNot Called }
        }

        @Test
        @DisplayName("Admin 도메인 + 관리자 API 허용 - 통과")
        fun adminDomain_adminApi_passes() {
            val request = createRequest(adminDomain, "/api/v1/admin/dashboard")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify { filterChain.doFilter(request, response) }
        }

        @Test
        @DisplayName("Admin 도메인 + 모바일 API 차단 - 404")
        fun adminDomain_mobileApi_blocked() {
            val request = createRequest(adminDomain, "/api/v1/mobile/home/dashboard")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            assertThat(response.status).isEqualTo(404)
            verify { filterChain wasNot Called }
        }

        @Test
        @DisplayName("Admin 도메인 + SPA 라우트 - 통과")
        fun adminDomain_spaRoute_passes() {
            val request = createRequest(adminDomain, "/sales/monthly")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify { filterChain.doFilter(request, response) }
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

            verify { filterChain.doFilter(request, response) }
        }

        @Test
        @DisplayName("IP 주소 (ALB health check) - 통과")
        fun ipAddress_passes() {
            val request = createRequest("10.0.1.50", "/api/health")
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, filterChain)

            verify { filterChain.doFilter(request, response) }
        }
    }

    @Nested
    @DisplayName("도메인 미설정 (빈 문자열)")
    inner class EmptyDomainTests {

        @Test
        @DisplayName("도메인 미설정 시 모든 경로 통과")
        fun emptyDomain_allPaths_pass() {
            val emptyFilter = DomainGuardFilter("", "")
            val request = createRequest("any-host.com", "/sales/monthly")
            val response = MockHttpServletResponse()

            emptyFilter.doFilter(request, response, filterChain)

            verify { filterChain.doFilter(request, response) }
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
            verify { filterChain wasNot Called }
        }
    }

    private fun createRequest(host: String, path: String): MockHttpServletRequest {
        val request = MockHttpServletRequest()
        request.addHeader("Host", host)
        request.requestURI = path
        return request
    }
}
