package com.otoki.powersales.common.controller

import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.auth.service.AuthService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminController 테스트")
class AdminControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @Nested
    @DisplayName("POST /api/v1/admin/users/{employeeCode}/reset-device - 단말기 초기화")
    inner class ResetDevice {

        @Test
        @DisplayName("성공 - ADMIN 권한으로 단말기 초기화")
        fun resetDevice_success() {
            // Given
            setSecurityContext(UserRole.BRANCH_MANAGER)
            doNothing().`when`(authService).resetDevice("20010585")

            // When & Then
            mockMvc.perform(post("/api/v1/admin/users/20010585/reset-device"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("단말기 등록이 초기화되었습니다"))

            verify(authService).resetDevice("20010585")
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사번 시 404 USER_NOT_FOUND")
        fun resetDevice_userNotFound() {
            // Given
            setSecurityContext(UserRole.BRANCH_MANAGER)
            doThrow(EmployeeNotFoundException()).`when`(authService).resetDevice("99999999")

            // When & Then
            mockMvc.perform(post("/api/v1/admin/users/99999999/reset-device"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"))
        }
    }

    private fun setSecurityContext(role: UserRole) {
        val principal = UserPrincipal(userId = 1L, role = role)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }
}
