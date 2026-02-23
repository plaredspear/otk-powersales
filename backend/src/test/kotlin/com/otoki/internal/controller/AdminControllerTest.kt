package com.otoki.internal.controller

import com.otoki.internal.entity.UserRole
import com.otoki.internal.exception.UserNotFoundException
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.AuthService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
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
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @Nested
    @DisplayName("POST /api/v1/admin/users/{employeeId}/reset-device - 단말기 초기화")
    inner class ResetDevice {

        @Test
        @DisplayName("성공 - ADMIN 권한으로 단말기 초기화")
        fun resetDevice_success() {
            // Given
            setSecurityContext(UserRole.ADMIN)
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
            setSecurityContext(UserRole.ADMIN)
            doThrow(UserNotFoundException()).`when`(authService).resetDevice("99999999")

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
