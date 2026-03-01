package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.AdminUserDetails
import com.otoki.internal.admin.service.AdminUserDetailsService
import com.otoki.internal.common.entity.UserRole
import com.otoki.internal.common.security.GpsConsentFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(AdminWebController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminWebController 테스트")
class AdminWebControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var adminUserDetailsService: AdminUserDetailsService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @Nested
    @DisplayName("GET /admin/login - 로그인 페이지")
    inner class LoginPageTests {

        @Test
        @DisplayName("정상 - 로그인 페이지 렌더링")
        fun loginPage_success() {
            mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk)
                .andExpect(view().name("admin/login"))
        }

        @Test
        @DisplayName("에러 파라미터 - error 메시지 포함")
        fun loginPage_withError() {
            mockMvc.perform(get("/admin/login").param("error", ""))
                .andExpect(status().isOk)
                .andExpect(model().attributeExists("errorMessage"))
        }

        @Test
        @DisplayName("로그아웃 파라미터 - logout 메시지 포함")
        fun loginPage_withLogout() {
            mockMvc.perform(get("/admin/login").param("logout", ""))
                .andExpect(status().isOk)
                .andExpect(model().attributeExists("logoutMessage"))
        }

        @Test
        @DisplayName("권한 부족 파라미터 - denied 메시지 포함")
        fun loginPage_withDenied() {
            mockMvc.perform(get("/admin/login").param("denied", ""))
                .andExpect(status().isOk)
                .andExpect(model().attributeExists("deniedMessage"))
        }
    }

    @Nested
    @DisplayName("GET /admin/dashboard - 대시보드")
    inner class DashboardTests {

        @BeforeEach
        fun setUpAuth() {
            val principal = AdminUserDetails(
                userId = 1L,
                employeeId = "20030117",
                displayName = "홍길동",
                encodedPassword = "dummy",
                role = UserRole.LEADER,
                enabled = true
            )
            SecurityContextHolder.getContext().authentication =
                UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        }

        @Test
        @DisplayName("정상 - 대시보드 페이지 렌더링 + 사용자 정보")
        fun dashboard_success() {
            mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk)
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("userName", "홍길동"))
                .andExpect(model().attribute("userRole", "LEADER"))
        }
    }
}
