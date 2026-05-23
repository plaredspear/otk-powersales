package com.otoki.powersales.common.security

import tools.jackson.databind.json.JsonMapper
import com.otoki.powersales.auth.entity.AppAuthority
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

@DisplayName("PasswordChangeRequiredFilter 테스트 (Spec #584)")
class PasswordChangeRequiredFilterTest {

    private val filterChain: FilterChain = mockk(relaxUnitFun = true)

    private lateinit var filter: PasswordChangeRequiredFilter
    private lateinit var request: MockHttpServletRequest
    private lateinit var response: MockHttpServletResponse

    @BeforeEach
    fun setUp() {
        val objectMapper = JsonMapper.builder().build()
        filter = PasswordChangeRequiredFilter(objectMapper)
        request = MockHttpServletRequest()
        response = MockHttpServletResponse()
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("강제 변경 미완료 + 일반 모바일 API -> 403")
    fun forceChangeRequired_blocksMobileApi() {
        val principal = principal(passwordChangeRequired = true)
        setAuthentication(principal)
        request.requestURI = "/api/v1/mobile/home/summary"

        filter.doFilter(request, response, filterChain)

        assertThat(response.status).isEqualTo(403)
        assertThat(response.contentAsString).contains("AUTH_PASSWORD_CHANGE_REQUIRED")
        verify { filterChain wasNot Called }
    }

    @Test
    @DisplayName("강제 변경 미완료 + change-password 화이트리스트 -> 통과")
    fun forceChangeRequired_passesChangePassword() {
        val principal = principal(passwordChangeRequired = true)
        setAuthentication(principal)
        request.requestURI = "/api/v1/mobile/auth/change-password"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
        assertThat(response.status).isEqualTo(200)
    }

    @Test
    @DisplayName("강제 변경 미완료 + logout 화이트리스트 -> 통과")
    fun forceChangeRequired_passesLogout() {
        val principal = principal(passwordChangeRequired = true)
        setAuthentication(principal)
        request.requestURI = "/api/v1/mobile/auth/logout"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    @Test
    @DisplayName("강제 변경 미완료 + refresh 화이트리스트 -> 통과")
    fun forceChangeRequired_passesRefresh() {
        val principal = principal(passwordChangeRequired = true)
        setAuthentication(principal)
        request.requestURI = "/api/v1/mobile/auth/refresh"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    @Test
    @DisplayName("일반 토큰 (passwordChangeRequired=false) -> 통과")
    fun normalToken_passesThrough() {
        val principal = principal(passwordChangeRequired = false)
        setAuthentication(principal)
        request.requestURI = "/api/v1/mobile/home/summary"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    @Test
    @DisplayName("admin 경로 - 강제 변경 미완료여도 가드 비적용")
    fun adminPath_notGuarded() {
        val principal = principal(passwordChangeRequired = true)
        setAuthentication(principal)
        request.requestURI = "/api/v1/admin/employees"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    @Test
    @DisplayName("미인증 요청 - 가드 통과 (다음 필터에서 401 처리)")
    fun unauthenticated_passesThrough() {
        request.requestURI = "/api/v1/mobile/home/summary"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    private fun principal(passwordChangeRequired: Boolean) = UserPrincipal(
        userId = 1L,
        role = AppAuthority.WOMAN,
        agreementFlag = true,
        passwordChangeRequired = passwordChangeRequired
    )

    private fun setAuthentication(principal: UserPrincipal) {
        val authentication = UsernamePasswordAuthenticationToken(
            principal, null, principal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }
}
