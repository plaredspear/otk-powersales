package com.otoki.powersales.common.security

import tools.jackson.databind.json.JsonMapper
import com.otoki.powersales.platform.auth.entity.AppAuthority
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

@DisplayName("GpsConsentFilter 테스트")
class GpsConsentFilterTest {

    private val filterChain: FilterChain = mockk(relaxUnitFun = true)

    private lateinit var filter: GpsConsentFilter
    private lateinit var request: MockHttpServletRequest
    private lateinit var response: MockHttpServletResponse

    @BeforeEach
    fun setUp() {
        val objectMapper = JsonMapper.builder().build()
        filter = GpsConsentFilter(objectMapper)
        request = MockHttpServletRequest()
        response = MockHttpServletResponse()
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("동의 완료 사용자 - 요청 통과")
    fun consentedUser_passesThrough() {
        val principal = UserPrincipal(userId = 1L, role = AppAuthority.WOMAN, agreementFlag = true)
        setAuthentication(principal)
        request.requestURI = "/api/v1/mobile/notices"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    @Test
    @DisplayName("미동의 사용자 - 403 GPS_CONSENT_REQUIRED")
    fun unconsentedUser_returns403() {
        val principal = UserPrincipal(userId = 1L, role = AppAuthority.WOMAN, agreementFlag = false)
        setAuthentication(principal)
        request.requestURI = "/api/v1/mobile/notices"

        filter.doFilter(request, response, filterChain)

        assertThat(response.status).isEqualTo(403)
        assertThat(response.contentAsString).contains("GPS_CONSENT_REQUIRED")
        verify { filterChain wasNot Called }
    }

    @Test
    @DisplayName("면제 엔드포인트 - /auth/gps-consent/terms 미동의여도 통과")
    fun exemptPath_gpsConsentTerms_passesThrough() {
        val principal = UserPrincipal(userId = 1L, role = AppAuthority.WOMAN, agreementFlag = false)
        setAuthentication(principal)
        request.requestURI = "/api/v1/mobile/auth/gps-consent/terms"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    @Test
    @DisplayName("면제 엔드포인트 - /auth/login 통과")
    fun exemptPath_login_passesThrough() {
        val principal = UserPrincipal(userId = 1L, role = AppAuthority.WOMAN, agreementFlag = false)
        setAuthentication(principal)
        request.requestURI = "/api/v1/mobile/auth/login"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    @Test
    @DisplayName("면제 엔드포인트 - /auth/change-password 통과")
    fun exemptPath_changePassword_passesThrough() {
        val principal = UserPrincipal(userId = 1L, role = AppAuthority.WOMAN, agreementFlag = false)
        setAuthentication(principal)
        request.requestURI = "/api/v1/mobile/auth/change-password"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    @Test
    @DisplayName("면제 엔드포인트 - /admin/ 경로 미동의여도 통과")
    fun exemptPath_admin_passesThrough() {
        val principal = UserPrincipal(userId = 1L, role = AppAuthority.WOMAN, agreementFlag = false)
        setAuthentication(principal)
        request.requestURI = "/api/v1/admin/notices"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    @Test
    @DisplayName("미인증 요청 - 필터 스킵")
    fun unauthenticatedRequest_passesThrough() {
        request.requestURI = "/api/v1/mobile/notices"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    private fun setAuthentication(principal: UserPrincipal) {
        val authentication = UsernamePasswordAuthenticationToken(
            principal, null, principal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }
}
