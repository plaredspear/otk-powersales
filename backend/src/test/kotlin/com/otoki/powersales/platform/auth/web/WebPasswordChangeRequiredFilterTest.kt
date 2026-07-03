package com.otoki.powersales.platform.auth.web

import tools.jackson.databind.json.JsonMapper
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

@DisplayName("WebPasswordChangeRequiredFilter 테스트")
class WebPasswordChangeRequiredFilterTest {

    private val filterChain: FilterChain = mockk(relaxUnitFun = true)

    private lateinit var filter: WebPasswordChangeRequiredFilter
    private lateinit var request: MockHttpServletRequest
    private lateinit var response: MockHttpServletResponse

    @BeforeEach
    fun setUp() {
        val objectMapper = JsonMapper.builder().build()
        filter = WebPasswordChangeRequiredFilter(objectMapper)
        request = MockHttpServletRequest()
        response = MockHttpServletResponse()
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("강제 변경 미완료 + 일반 admin API -> 403")
    fun forceChangeRequired_blocksAdminApi() {
        setAuthentication(principal(passwordChangeRequired = true))
        request.requestURI = "/api/v1/admin/employees"

        filter.doFilter(request, response, filterChain)

        assertThat(response.status).isEqualTo(403)
        assertThat(response.contentAsString).contains("AUTH_PASSWORD_CHANGE_REQUIRED")
        verify { filterChain wasNot Called }
    }

    @Test
    @DisplayName("강제 변경 미완료 + 비밀번호 변경 endpoint 화이트리스트 -> 통과")
    fun forceChangeRequired_passesChangePassword() {
        setAuthentication(principal(passwordChangeRequired = true))
        request.requestURI = "/api/v1/admin/auth/password"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
        assertThat(response.status).isEqualTo(200)
    }

    @Test
    @DisplayName("강제 변경 미완료 + refresh 화이트리스트 -> 통과")
    fun forceChangeRequired_passesRefresh() {
        setAuthentication(principal(passwordChangeRequired = true))
        request.requestURI = "/api/v1/admin/auth/refresh"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    @Test
    @DisplayName("강제 변경 미완료 + login 화이트리스트 -> 통과")
    fun forceChangeRequired_passesLogin() {
        setAuthentication(principal(passwordChangeRequired = true))
        request.requestURI = "/api/v1/admin/auth/login"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    @Test
    @DisplayName("일반 principal (passwordChangeRequired=false) -> 통과")
    fun normalPrincipal_passesThrough() {
        setAuthentication(principal(passwordChangeRequired = false))
        request.requestURI = "/api/v1/admin/employees"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    @Test
    @DisplayName("대행 중 (impersonatedBy != null) + 강제 변경 미완료 -> 통과")
    fun impersonating_passesThrough() {
        setAuthentication(principal(passwordChangeRequired = true, impersonatedBy = 7L))
        request.requestURI = "/api/v1/admin/employees"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    @Test
    @DisplayName("mobile 경로 - 강제 변경 미완료여도 본 필터는 가드 비적용")
    fun mobilePath_notGuarded() {
        setAuthentication(principal(passwordChangeRequired = true))
        request.requestURI = "/api/v1/mobile/home/summary"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    @Test
    @DisplayName("미인증 요청 - 가드 통과 (다음 필터에서 401 처리)")
    fun unauthenticated_passesThrough() {
        request.requestURI = "/api/v1/admin/employees"

        filter.doFilter(request, response, filterChain)

        verify { filterChain.doFilter(request, response) }
    }

    private fun principal(passwordChangeRequired: Boolean, impersonatedBy: Long? = null) = WebUserPrincipal(
        userId = 1L,
        usernameValue = "u@otokims.co.kr",
        employeeCode = "S001",
        employeeId = null,
        role = null,
        costCenterCode = null,
        isSalesSupport = false,
        passwordChangeRequired = passwordChangeRequired,
        permissions = emptySet(),
        impersonatedBy = impersonatedBy,
        encodedPassword = "",
        grantedAuthorities = emptyList(),
        active = true
    )

    private fun setAuthentication(principal: WebUserPrincipal) {
        val authentication = UsernamePasswordAuthenticationToken(
            principal, null, principal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }
}
