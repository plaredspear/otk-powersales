package com.otoki.powersales.auth.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.auth.dto.request.LoginRequest
import com.otoki.powersales.auth.dto.response.*
import com.otoki.powersales.common.dto.response.*
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.auth.exception.InvalidCredentialsException
import com.otoki.powersales.auth.exception.InvalidCurrentPasswordException
import com.otoki.powersales.auth.exception.InvalidTokenException
import com.otoki.powersales.auth.exception.NewPasswordPolicyViolationException
import com.otoki.powersales.auth.exception.NewPasswordSameAsTemporaryException
import com.otoki.powersales.auth.exception.TermsNotFoundException
import com.otoki.powersales.auth.exception.TokenReuseDetectedException
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.common.security.PasswordChangeRequiredFilter
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.auth.service.AuthService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
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

    @MockkBean
    private lateinit var authService: AuthService

    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockkBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockkBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @MockkBean
    private lateinit var passwordChangeRequiredFilter: PasswordChangeRequiredFilter

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.WOMAN)

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
        val request = LoginRequest(employeeCode = "12345678", password = "password123")
        val mockResponse = LoginResponse(
            user = UserInfo(1L, "12345678", "홍길동", "서울지점", "WOMAN", "여사원"),
            token = TokenInfo("access-token", "refresh-token", 3600),
            passwordChangeRequired = false,
            requiresGpsConsent = false
        )

        every { authService.login(any()) } returns mockResponse

        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("로그인 성공"))
            .andExpect(jsonPath("$.data.user.id").value(1))
            .andExpect(jsonPath("$.data.user.employeeCode").value("12345678"))
            .andExpect(jsonPath("$.data.user.name").value("홍길동"))
            .andExpect(jsonPath("$.data.user.orgName").value("서울지점"))
            .andExpect(jsonPath("$.data.user.role").value("WOMAN"))
            .andExpect(jsonPath("$.data.token.accessToken").value("access-token"))
            .andExpect(jsonPath("$.data.token.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.data.token.expiresIn").value(3600))
            .andExpect(jsonPath("$.data.passwordChangeRequired").value(false))
            .andExpect(jsonPath("$.data.requiresGpsConsent").value(false))
    }

    @Test
    @DisplayName("초기 비밀번호로 로그인 - 200 OK, password_change_required=true")
    fun login_initialPassword() {
        // Given
        val mockResponse = LoginResponse(
            user = UserInfo(2L, "87654321", "김철수", "부산지점", "WOMAN", "여사원"),
            token = TokenInfo("access-token", "refresh-token", 3600),
            passwordChangeRequired = true,
            requiresGpsConsent = true
        )

        every { authService.login(any()) } returns mockResponse

        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"employeeCode": "87654321", "password": "otg1"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.passwordChangeRequired").value(true))
            .andExpect(jsonPath("$.data.requiresGpsConsent").value(true))
    }

    @Test
    @DisplayName("사번 없음 - 401 INVALID_CREDENTIALS")
    fun login_invalidCredentials() {
        // Given
        every { authService.login(any()) } throws InvalidCredentialsException()

        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"employeeCode": "99999999", "password": "password123"}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
    }

    @Test
    @DisplayName("필수 필드 누락 (employeeCode 빈 값) - 400 INVALID_PARAMETER")
    fun login_missingEmployeeCode() {
        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"employeeCode": "", "password": "password123"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
    }

    @Test
    @DisplayName("사번 형식 오류 (6자리) - 400 with validation message")
    fun login_invalidEmployeeCodeFormat() {
        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"employeeCode": "123456", "password": "password123"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
            .andExpect(jsonPath("$.error.message", containsString("사번은 8자리 숫자 또는 'ADMIN-'")))
    }

    @Test
    @DisplayName("비밀번호 길이 부족 - 400 with validation message")
    fun login_shortPassword() {
        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"employeeCode": "12345678", "password": "123"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
            .andExpect(jsonPath("$.error.message", containsString("4글자 이상")))
    }

    // ========== Token Refresh Tests ==========

    @Test
    @DisplayName("토큰 갱신 성공 - 200 OK, access_token + refresh_token 반환")
    fun refresh_success() {
        // Given
        val mockResponse = TokenResponse(
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token",
            expiresIn = 3600
        )

        every { authService.refreshAccessToken(any()) } returns mockResponse

        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken": "valid-refresh-token"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("토큰 갱신 성공"))
            .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
            .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
            .andExpect(jsonPath("$.data.expiresIn").value(3600))
    }

    @Test
    @DisplayName("탈취 감지 - 재사용된 Refresh Token 시 401 TOKEN_REUSE_DETECTED")
    fun refresh_tokenReuseDetected() {
        // Given
        every { authService.refreshAccessToken(any()) } throws TokenReuseDetectedException()

        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken": "reused-refresh-token"}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("TOKEN_REUSE_DETECTED"))
    }

    @Test
    @DisplayName("유효하지 않은 토큰 - 변조된 Refresh Token 시 401 INVALID_TOKEN")
    fun refresh_invalidToken() {
        // Given
        every { authService.refreshAccessToken(any()) } throws InvalidTokenException()

        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken": "tampered-token"}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"))
    }

    @Test
    @DisplayName("Refresh Token 필수 필드 누락 - 400 INVALID_PARAMETER")
    fun refresh_missingToken() {
        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken": ""}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
    }

    // ========== Change Password Tests ==========

    @Test
    @DisplayName("비밀번호 변경 성공 - 200 OK + 새 토큰")
    fun changePassword_success() {
        // Given
        val mockResponse = ChangePasswordResponse("new-access", "new-refresh", 3600)
        every { authService.changePassword(any<UserPrincipal>(), any()) } returns mockResponse

        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword": "oldPass", "newPassword": "newPass1"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("비밀번호가 변경되었습니다"))
            .andExpect(jsonPath("$.data.accessToken").value("new-access"))
            .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"))
    }

    @Test
    @DisplayName("비밀번호 변경 - 현재 비밀번호 불일치 시 401 AUTH_CURRENT_PASSWORD_MISMATCH")
    fun changePassword_wrongCurrentPassword() {
        // Given
        every { authService.changePassword(any<UserPrincipal>(), any()) } throws InvalidCurrentPasswordException()

        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword": "wrongPass", "newPassword": "newPass1"}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("AUTH_CURRENT_PASSWORD_MISMATCH"))
    }

    @Test
    @DisplayName("비밀번호 변경 - 새 비밀번호 누락 시 400 INVALID_PARAMETER")
    fun changePassword_missingNewPassword() {
        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword": "oldPass123", "newPassword": ""}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
    }

    @Test
    @DisplayName("비밀번호 변경 - 정책 위반 시 400 AUTH_NEW_PASSWORD_INVALID + violations 배열")
    fun changePassword_policyViolation() {
        // Given
        every { authService.changePassword(any<UserPrincipal>(), any()) } throws
            NewPasswordPolicyViolationException(listOf("LENGTH_TOO_SHORT"))

        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"newPassword": "abc"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("AUTH_NEW_PASSWORD_INVALID"))
            .andExpect(jsonPath("$.error.details.violations[0]").value("LENGTH_TOO_SHORT"))
    }

    @Test
    @DisplayName("비밀번호 변경 - 임시 비번 동일 시 400 AUTH_NEW_PASSWORD_SAME_AS_TEMP")
    fun changePassword_sameAsTemporary() {
        // Given
        every { authService.changePassword(any<UserPrincipal>(), any()) } throws NewPasswordSameAsTemporaryException()

        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"newPassword": "1234"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("AUTH_NEW_PASSWORD_SAME_AS_TEMP"))
    }

    // ========== Verify Password Tests ==========

    @Test
    @DisplayName("비밀번호 검증 성공 - 200 OK (인증 필요)")
    fun verifyPassword_success() {
        // Given
        every { authService.verifyPassword(1L, any()) } just Runs

        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword": "correctPass123"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("비밀번호가 확인되었습니다"))
            .andExpect(jsonPath("$.data").isEmpty)
    }

    @Test
    @DisplayName("비밀번호 검증 실패 - 비밀번호 불일치 시 401 AUTH_CURRENT_PASSWORD_MISMATCH")
    fun verifyPassword_passwordMismatch() {
        // Given
        every { authService.verifyPassword(1L, any()) } throws InvalidCurrentPasswordException()

        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword": "wrongPass"}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("AUTH_CURRENT_PASSWORD_MISMATCH"))
    }

    @Test
    @DisplayName("비밀번호 검증 - 비밀번호 필드 누락 시 400")
    fun verifyPassword_missingPassword() {
        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"password": ""}""")
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
        every { authService.logout(any()) } just Runs

        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/logout")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isNoContent)

        verify { authService.logout(accessToken) }
    }

    @Test
    @DisplayName("로그아웃 - Authorization 헤더 없이도 204 반환")
    fun logout_withoutAuthHeader() {
        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/logout")
        )
            .andExpect(status().isNoContent)
    }

    // ========== GPS Consent Terms Tests ==========

    @Test
    @DisplayName("GPS 약관 조회 성공 - 200 OK")
    fun gpsConsentTerms_success() {
        // Given
        val response = GpsConsentTermsResponse("AGR-2025-001", "약관 본문 텍스트")
        every { authService.getGpsConsentTerms() } returns response

        // When & Then
        mockMvc.perform(get("/api/v1/mobile/auth/gps-consent/terms"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.agreementNumber").value("AGR-2025-001"))
            .andExpect(jsonPath("$.data.contents").value("약관 본문 텍스트"))
    }

    @Test
    @DisplayName("GPS 약관 조회 실패 - 활성 약관 없음 404")
    fun gpsConsentTerms_notFound() {
        // Given
        every { authService.getGpsConsentTerms() } throws TermsNotFoundException()

        // When & Then
        mockMvc.perform(get("/api/v1/mobile/auth/gps-consent/terms"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("TERMS_NOT_FOUND"))
    }

    // ========== GPS Consent Status Tests ==========

    @Test
    @DisplayName("GPS 동의 상태 조회 - 동의 필요")
    fun gpsConsentStatus_requiresConsent() {
        // Given
        val response = GpsConsentStatusResponse(requiresGpsConsent = true)
        every { authService.getGpsConsentStatus(1L) } returns response

        // When & Then
        mockMvc.perform(get("/api/v1/mobile/auth/gps-consent/status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.requiresGpsConsent").value(true))
    }

    @Test
    @DisplayName("GPS 동의 상태 조회 - 동의 불필요")
    fun gpsConsentStatus_consentGiven() {
        // Given
        val response = GpsConsentStatusResponse(requiresGpsConsent = false)
        every { authService.getGpsConsentStatus(1L) } returns response

        // When & Then
        mockMvc.perform(get("/api/v1/mobile/auth/gps-consent/status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.requiresGpsConsent").value(false))
    }

    // ========== GPS Consent Record Tests ==========

    @Test
    @DisplayName("GPS 동의 기록 성공 - 약관번호 포함, 새 토큰 반환")
    fun gpsConsent_success_withAgreementNumber() {
        // Given
        val response = GpsConsentRecordResponse(accessToken = "new-token", expiresIn = 3600)
        every { authService.recordGpsConsent(1L, any()) } returns response

        // When & Then
        mockMvc.perform(
            post("/api/v1/mobile/auth/gps-consent")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"agreementNumber": "AGR-2025-001"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("GPS 사용 동의가 기록되었습니다"))
            .andExpect(jsonPath("$.data.accessToken").value("new-token"))
            .andExpect(jsonPath("$.data.expiresIn").value(3600))
    }

    @Test
    @DisplayName("GPS 동의 기록 성공 - 요청 본문 없이 (하위 호환)")
    fun gpsConsent_success_withoutBody() {
        // Given
        val response = GpsConsentRecordResponse(accessToken = "new-token", expiresIn = 3600)
        every { authService.recordGpsConsent(1L, any()) } returns response

        // When & Then
        mockMvc.perform(post("/api/v1/mobile/auth/gps-consent"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("new-token"))
    }
}
