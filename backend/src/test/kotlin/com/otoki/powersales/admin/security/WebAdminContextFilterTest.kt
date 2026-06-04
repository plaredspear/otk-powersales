package com.otoki.powersales.admin.security

import com.otoki.powersales.auth.permission.AdminPermissionCache
import com.otoki.powersales.auth.permission.RequiresSfPermission
import com.otoki.powersales.auth.permission.SfPermissionEvaluator
import com.otoki.powersales.auth.permission.SfPermissionOperation
import com.otoki.powersales.auth.permission.SystemAdminProfilePolicy
import com.otoki.powersales.auth.web.WebUserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerExecutionChain
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@DisplayName("WebAdminContextFilter — 시스템 관리자 권한 우회")
class WebAdminContextFilterTest {

    private val adminDataScopeCache = mockk<AdminDataScopeCache>(relaxed = true)
    private val requestMappingHandlerMapping = mockk<RequestMappingHandlerMapping>()
    private val sfPermissionEvaluator = mockk<SfPermissionEvaluator>()
    private val adminPermissionCache = mockk<AdminPermissionCache>(relaxed = true)
    private val objectMapper = tools.jackson.databind.ObjectMapper()

    private val filter = WebAdminContextFilter(
        adminDataScopeCache = adminDataScopeCache,
        requestMappingHandlerMapping = requestMappingHandlerMapping,
        sfPermissionEvaluator = sfPermissionEvaluator,
        adminPermissionCache = adminPermissionCache,
        objectMapper = objectMapper,
    )

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    @DisplayName("시스템 관리자 — 빈 권한 셋이어도 @RequiresSfPermission endpoint 통과 (평가자 미호출)")
    fun systemAdminBypassesPermissionCheck() {
        authenticate(principal(profileName = SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME, permissions = emptySet()))
        stubHandlerWithRequiresPermission(entity = "employee", operation = SfPermissionOperation.EDIT)
        // SF 권한으로는 막힌 상태(false)여도 시스템 관리자 fallback 으로 통과해야 함
        every { sfPermissionEvaluator.isAllowed(any(), any()) } returns false
        val chain = mockk<FilterChain>(relaxed = true)
        val response = MockHttpServletResponse()

        filter.doFilter(MockHttpServletRequest("GET", "/api/v1/admin/employees"), response, chain)

        verify(exactly = 1) { chain.doFilter(any(), any()) }
        assertThat(response.status).isEqualTo(HttpServletResponse.SC_OK)
    }

    @Test
    @DisplayName("시스템 관리자 — employeeId==null 부트스트랩(빈 권한)이어도 통과 (profileName 기반 우회)")
    fun systemAdminBootstrapBypassesEvenWithoutEmployee() {
        authenticate(
            principal(
                profileName = SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME,
                permissions = emptySet(),
                employeeId = null,
            )
        )
        stubHandlerWithRequiresPermission(entity = "employee", operation = SfPermissionOperation.EDIT)
        every { sfPermissionEvaluator.isAllowed(any(), any()) } returns false
        val chain = mockk<FilterChain>(relaxed = true)
        val response = MockHttpServletResponse()

        filter.doFilter(MockHttpServletRequest("GET", "/api/v1/admin/employees"), response, chain)

        verify(exactly = 1) { chain.doFilter(any(), any()) }
        assertThat(response.status).isEqualTo(HttpServletResponse.SC_OK)
    }

    @Test
    @DisplayName("profileName==null — 우회 안 됨, 권한 부재 시 403")
    fun nullProfileNameDoesNotBypass() {
        authenticate(principal(profileName = null, permissions = emptySet()))
        stubHandlerWithRequiresPermission(entity = "employee", operation = SfPermissionOperation.EDIT)
        every { sfPermissionEvaluator.isAllowed(any(), any()) } returns false
        val chain = mockk<FilterChain>(relaxed = true)
        val response = MockHttpServletResponse()

        filter.doFilter(MockHttpServletRequest("GET", "/api/v1/admin/employees"), response, chain)

        verify(exactly = 0) { chain.doFilter(any(), any()) }
        assertThat(response.status).isEqualTo(HttpServletResponse.SC_FORBIDDEN)
    }

    @Test
    @DisplayName("일반 사용자 — 권한 부재 시 403 (평가자 차단)")
    fun nonAdminBlockedWhenPermissionMissing() {
        authenticate(principal(profileName = "5.영업사원", permissions = emptySet()))
        stubHandlerWithRequiresPermission(entity = "employee", operation = SfPermissionOperation.EDIT)
        every { sfPermissionEvaluator.isAllowed(any(), any()) } returns false
        val chain = mockk<FilterChain>(relaxed = true)
        val response = MockHttpServletResponse()

        filter.doFilter(MockHttpServletRequest("GET", "/api/v1/admin/employees"), response, chain)

        verify(exactly = 0) { chain.doFilter(any(), any()) }
        assertThat(response.status).isEqualTo(HttpServletResponse.SC_FORBIDDEN)
    }

    @Test
    @DisplayName("일반 사용자 — 권한 보유 시 통과")
    fun nonAdminPassesWhenPermissionGranted() {
        authenticate(principal(profileName = "5.영업사원", permissions = setOf("employee:E")))
        stubHandlerWithRequiresPermission(entity = "employee", operation = SfPermissionOperation.EDIT)
        every { sfPermissionEvaluator.isAllowed(any(), any()) } returns true
        val chain = mockk<FilterChain>(relaxed = true)
        val response = MockHttpServletResponse()

        filter.doFilter(MockHttpServletRequest("GET", "/api/v1/admin/employees"), response, chain)

        verify(exactly = 1) { chain.doFilter(any(), any()) }
        assertThat(response.status).isEqualTo(HttpServletResponse.SC_OK)
    }

    private fun stubHandlerWithRequiresPermission(entity: String, operation: SfPermissionOperation) {
        val annotation = mockk<RequiresSfPermission>()
        every { annotation.entity } returns entity
        every { annotation.operation } returns operation
        val handlerMethod = mockk<HandlerMethod>()
        every { handlerMethod.getMethodAnnotation(RequiresSfPermission::class.java) } returns annotation
        val executionChain = mockk<HandlerExecutionChain>()
        every { executionChain.handler } returns handlerMethod
        every { requestMappingHandlerMapping.getHandler(any()) } returns executionChain
    }

    private fun authenticate(principal: WebUserPrincipal) {
        val auth = UsernamePasswordAuthenticationToken(principal, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }

    private fun principal(
        profileName: String?,
        permissions: Set<String>,
        employeeId: Long? = 1L,
    ): WebUserPrincipal {
        val mock = mockk<WebUserPrincipal>()
        every { mock.employeeId } returns employeeId
        every { mock.userId } returns 1L
        every { mock.profileName } returns profileName
        every { mock.permissions } returns permissions
        return mock
    }
}
