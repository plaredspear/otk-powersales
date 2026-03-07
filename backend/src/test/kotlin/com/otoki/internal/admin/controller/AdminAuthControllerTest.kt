package com.otoki.internal.admin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.admin.dto.response.AdminLoginResponse
import com.otoki.internal.admin.dto.response.AdminTokenInfo
import com.otoki.internal.admin.dto.response.AdminUserInfo
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.auth.dto.response.TokenResponse
import com.otoki.internal.auth.exception.InvalidCredentialsException
import com.otoki.internal.auth.exception.WebLoginNotAllowedException
import com.otoki.internal.auth.service.AuthService
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminAuthController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminAuthController 테스트")
class AdminAuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @Nested
    @DisplayName("POST /api/v1/admin/auth/login - 관리자 로그인")
    inner class AdminLoginTests {

        @Test
        @DisplayName("성공 - 관리자 권한 사번으로 로그인 시 200 + AdminLoginResponse 반환")
        fun adminLogin_success() {
            // Given
            val mockResponse = AdminLoginResponse(
                user = AdminUserInfo(1L, "00000001", "홍길동", "서울지점", "LEADER", "조장", "CC001"),
                token = AdminTokenInfo("access-token", "refresh-token", 3600)
            )
            whenever(authService.adminLogin(any())).thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                post("/api/v1/admin/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"employee_id": "00000001", "password": "1234"}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("관리자 로그인 성공"))
                .andExpect(jsonPath("$.data.user.id").value(1))
                .andExpect(jsonPath("$.data.user.employee_id").value("00000001"))
                .andExpect(jsonPath("$.data.user.name").value("홍길동"))
                .andExpect(jsonPath("$.data.user.org_name").value("서울지점"))
                .andExpect(jsonPath("$.data.user.role").value("LEADER"))
                .andExpect(jsonPath("$.data.user.app_authority").value("조장"))
                .andExpect(jsonPath("$.data.user.cost_center_code").value("CC001"))
                .andExpect(jsonPath("$.data.token.access_token").value("access-token"))
                .andExpect(jsonPath("$.data.token.refresh_token").value("refresh-token"))
                .andExpect(jsonPath("$.data.token.expires_in").value(3600))
        }

        @Test
        @DisplayName("실패 - 사번/비밀번호 불일치 시 401 INVALID_CREDENTIALS")
        fun adminLogin_invalidCredentials() {
            // Given
            whenever(authService.adminLogin(any())).thenThrow(InvalidCredentialsException())

            // When & Then
            mockMvc.perform(
                post("/api/v1/admin/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"employee_id": "00000001", "password": "wrong"}""")
            )
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
        }

        @Test
        @DisplayName("실패 - 관리자 권한 없음 시 403 WEB_LOGIN_NOT_ALLOWED")
        fun adminLogin_notAllowed() {
            // Given
            whenever(authService.adminLogin(any())).thenThrow(WebLoginNotAllowedException())

            // When & Then
            mockMvc.perform(
                post("/api/v1/admin/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"employee_id": "00000002", "password": "1234"}""")
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("WEB_LOGIN_NOT_ALLOWED"))
        }

        @Test
        @DisplayName("실패 - 사번 형식 오류 시 400 validation 에러")
        fun adminLogin_invalidEmployeeIdFormat() {
            // When & Then
            mockMvc.perform(
                post("/api/v1/admin/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"employee_id": "abc", "password": "1234"}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.error.message", containsString("사번은 8자리")))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/auth/refresh - 관리자 토큰 갱신")
    inner class AdminRefreshTests {

        @Test
        @DisplayName("성공 - 유효한 refresh token으로 새 토큰 반환")
        fun adminRefresh_success() {
            // Given
            val mockResponse = TokenResponse("new-access-token", "new-refresh-token", 3600)
            whenever(authService.refreshAccessToken(any())).thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                post("/api/v1/admin/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"refresh_token": "valid-refresh-token"}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("토큰 갱신 성공"))
                .andExpect(jsonPath("$.data.access_token").value("new-access-token"))
                .andExpect(jsonPath("$.data.refresh_token").value("new-refresh-token"))
                .andExpect(jsonPath("$.data.expires_in").value(3600))
        }
    }
}
