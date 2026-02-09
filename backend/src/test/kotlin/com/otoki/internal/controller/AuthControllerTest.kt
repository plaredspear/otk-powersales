package com.otoki.internal.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.dto.request.ChangePasswordRequest
import com.otoki.internal.dto.request.LoginRequest
import com.otoki.internal.dto.request.RefreshTokenRequest
import com.otoki.internal.dto.response.LoginResponse
import com.otoki.internal.dto.response.TokenInfo
import com.otoki.internal.dto.response.TokenResponse
import com.otoki.internal.dto.response.UserInfo
import com.otoki.internal.entity.UserRole
import com.otoki.internal.exception.InvalidCredentialsException
import com.otoki.internal.exception.InvalidCurrentPasswordException
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.AuthService
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * AuthController 테스트
 * addFilters=false로 Security 필터 비활성화, 직접 SecurityContext 설정
 */
@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

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

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.USER)

    @BeforeEach
    fun setUp() {
        // 인증된 사용자 SecurityContext 설정
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    // ========== Login Tests ==========

    @Test
    @DisplayName("정상 로그인 - 200 OK, 사용자 정보 및 토큰 반환")
    fun login_success() {
        // Given
        val request = LoginRequest(employeeId = "12345678", password = "password123")
        val mockResponse = LoginResponse(
            user = UserInfo(1L, "12345678", "홍길동", "영업1팀", "서울지점", "USER"),
            token = TokenInfo("access-token", "refresh-token", 3600),
            requiresPasswordChange = false,
            requiresGpsConsent = false
        )

        whenever(authService.login(any())).thenReturn(mockResponse)

        // When & Then
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("로그인 성공"))
            .andExpect(jsonPath("$.data.user.id").value(1))
            .andExpect(jsonPath("$.data.user.employee_id").value("12345678"))
            .andExpect(jsonPath("$.data.user.name").value("홍길동"))
            .andExpect(jsonPath("$.data.user.department").value("영업1팀"))
            .andExpect(jsonPath("$.data.user.branch_name").value("서울지점"))
            .andExpect(jsonPath("$.data.user.role").value("USER"))
            .andExpect(jsonPath("$.data.token.access_token").value("access-token"))
            .andExpect(jsonPath("$.data.token.refresh_token").value("refresh-token"))
            .andExpect(jsonPath("$.data.token.expires_in").value(3600))
            .andExpect(jsonPath("$.data.requires_password_change").value(false))
            .andExpect(jsonPath("$.data.requires_gps_consent").value(false))
    }

    @Test
    @DisplayName("초기 비밀번호로 로그인 - 200 OK, requires_password_change=true")
    fun login_initialPassword() {
        // Given
        val mockResponse = LoginResponse(
            user = UserInfo(2L, "87654321", "김철수", "영업2팀", "부산지점", "USER"),
            token = TokenInfo("access-token", "refresh-token", 3600),
            requiresPasswordChange = true,
            requiresGpsConsent = true
        )

        whenever(authService.login(any())).thenReturn(mockResponse)

        // When & Then
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"employee_id": "87654321", "password": "otg1"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.requires_password_change").value(true))
            .andExpect(jsonPath("$.data.requires_gps_consent").value(true))
    }

    @Test
    @DisplayName("사번 없음 - 401 INVALID_CREDENTIALS")
    fun login_invalidCredentials() {
        // Given
        whenever(authService.login(any())).thenThrow(InvalidCredentialsException())

        // When & Then
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"employee_id": "99999999", "password": "password123"}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
    }

    @Test
    @DisplayName("필수 필드 누락 (employeeId 빈 값) - 400 INVALID_PARAMETER")
    fun login_missingEmployeeId() {
        // When & Then
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"employee_id": "", "password": "password123"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
    }

    @Test
    @DisplayName("사번 형식 오류 (6자리) - 400 with validation message")
    fun login_invalidEmployeeIdFormat() {
        // When & Then
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"employee_id": "123456", "password": "password123"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
            .andExpect(jsonPath("$.error.message", containsString("사번은 8자리")))
    }

    @Test
    @DisplayName("비밀번호 길이 부족 - 400 with validation message")
    fun login_shortPassword() {
        // When & Then
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"employee_id": "12345678", "password": "123"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
            .andExpect(jsonPath("$.error.message", containsString("4글자 이상")))
    }

    // ========== Token Refresh Tests ==========

    @Test
    @DisplayName("토큰 갱신 성공 - 200 OK")
    fun refresh_success() {
        // Given
        val mockResponse = TokenResponse(accessToken = "new-access-token", expiresIn = 3600)

        whenever(authService.refreshAccessToken(any())).thenReturn(mockResponse)

        // When & Then
        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refresh_token": "valid-refresh-token"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("토큰 갱신 성공"))
            .andExpect(jsonPath("$.data.access_token").value("new-access-token"))
            .andExpect(jsonPath("$.data.expires_in").value(3600))
    }

    @Test
    @DisplayName("Refresh Token 필수 필드 누락 - 400 INVALID_PARAMETER")
    fun refresh_missingToken() {
        // When & Then
        mockMvc.perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refresh_token": ""}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
    }

    // ========== Change Password Tests ==========

    @Test
    @DisplayName("비밀번호 변경 성공 - 200 OK (인증 필요)")
    fun changePassword_success() {
        // Given
        doNothing().whenever(authService).changePassword(eq(1L), any())

        // When & Then
        mockMvc.perform(
            post("/api/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"current_password": "oldPass", "new_password": "newPass1"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("비밀번호가 변경되었습니다"))
    }

    @Test
    @DisplayName("비밀번호 변경 - 현재 비밀번호 불일치 시 401")
    fun changePassword_wrongCurrentPassword() {
        // Given
        doThrow(InvalidCurrentPasswordException())
            .whenever(authService).changePassword(eq(1L), any())

        // When & Then
        mockMvc.perform(
            post("/api/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"current_password": "wrongPass", "new_password": "newPass1"}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_CURRENT_PASSWORD"))
    }

    @Test
    @DisplayName("비밀번호 변경 - 새 비밀번호 길이 부족 시 400")
    fun changePassword_shortNewPassword() {
        // When & Then
        mockMvc.perform(
            post("/api/v1/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"current_password": "oldPass123", "new_password": "12"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
    }

    // ========== Logout Tests ==========

    @Test
    @DisplayName("로그아웃 성공 - 204 No Content")
    fun logout_success() {
        // Given
        val accessToken = "test-access-token"
        doNothing().whenever(authService).logout(any())

        // When & Then
        mockMvc.perform(
            post("/api/v1/auth/logout")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isNoContent)

        verify(authService).logout(accessToken)
    }

    @Test
    @DisplayName("로그아웃 - Authorization 헤더 없이도 204 반환")
    fun logout_withoutAuthHeader() {
        // When & Then
        mockMvc.perform(
            post("/api/v1/auth/logout")
        )
            .andExpect(status().isNoContent)
    }

    // ========== GPS Consent Tests ==========

    @Test
    @DisplayName("GPS 동의 기록 성공 - 200 OK (인증 필요)")
    fun gpsConsent_success() {
        // Given
        doNothing().whenever(authService).recordGpsConsent(1L)

        // When & Then
        mockMvc.perform(
            post("/api/v1/auth/gps-consent")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("GPS 사용 동의가 기록되었습니다"))

        verify(authService).recordGpsConsent(1L)
    }
}
