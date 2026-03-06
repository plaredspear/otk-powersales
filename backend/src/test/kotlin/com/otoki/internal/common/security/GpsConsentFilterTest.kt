package com.otoki.internal.common.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.otoki.internal.sap.entity.UserRole
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

@ExtendWith(MockitoExtension::class)
@DisplayName("GpsConsentFilter 테스트")
class GpsConsentFilterTest {

    @Mock
    private lateinit var filterChain: FilterChain

    private lateinit var filter: GpsConsentFilter
    private lateinit var request: MockHttpServletRequest
    private lateinit var response: MockHttpServletResponse

    @BeforeEach
    fun setUp() {
        val objectMapper = ObjectMapper().apply {
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
            registerModule(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
        filter = GpsConsentFilter(objectMapper)
        request = MockHttpServletRequest()
        response = MockHttpServletResponse()
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("동의 완료 사용자 - 요청 통과")
    fun consentedUser_passesThrough() {
        // Given
        val principal = UserPrincipal(userId = 1L, role = UserRole.USER, agreementFlag = true)
        setAuthentication(principal)
        request.requestURI = "/api/v1/notices"

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        verify(filterChain).doFilter(request, response)
    }

    @Test
    @DisplayName("미동의 사용자 - 403 GPS_CONSENT_REQUIRED")
    fun unconsentedUser_returns403() {
        // Given
        val principal = UserPrincipal(userId = 1L, role = UserRole.USER, agreementFlag = false)
        setAuthentication(principal)
        request.requestURI = "/api/v1/notices"

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        assertThat(response.status).isEqualTo(403)
        assertThat(response.contentAsString).contains("GPS_CONSENT_REQUIRED")
        verifyNoInteractions(filterChain)
    }

    @Test
    @DisplayName("면제 엔드포인트 - /auth/gps-consent/terms 미동의여도 통과")
    fun exemptPath_gpsConsentTerms_passesThrough() {
        // Given
        val principal = UserPrincipal(userId = 1L, role = UserRole.USER, agreementFlag = false)
        setAuthentication(principal)
        request.requestURI = "/api/v1/auth/gps-consent/terms"

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        verify(filterChain).doFilter(request, response)
    }

    @Test
    @DisplayName("면제 엔드포인트 - /auth/login 통과")
    fun exemptPath_login_passesThrough() {
        // Given
        val principal = UserPrincipal(userId = 1L, role = UserRole.USER, agreementFlag = false)
        setAuthentication(principal)
        request.requestURI = "/api/v1/auth/login"

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        verify(filterChain).doFilter(request, response)
    }

    @Test
    @DisplayName("면제 엔드포인트 - /auth/change-password 통과")
    fun exemptPath_changePassword_passesThrough() {
        // Given
        val principal = UserPrincipal(userId = 1L, role = UserRole.USER, agreementFlag = false)
        setAuthentication(principal)
        request.requestURI = "/api/v1/auth/change-password"

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        verify(filterChain).doFilter(request, response)
    }

    @Test
    @DisplayName("미인증 요청 - 필터 스킵")
    fun unauthenticatedRequest_passesThrough() {
        // Given
        request.requestURI = "/api/v1/notices"

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        verify(filterChain).doFilter(request, response)
    }

    private fun setAuthentication(principal: UserPrincipal) {
        val authentication = UsernamePasswordAuthenticationToken(
            principal, null, principal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }
}
