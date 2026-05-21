package com.otoki.powersales.auth.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.auth.dto.request.LoginRequest
import com.otoki.powersales.auth.dto.response.*
import com.otoki.powersales.common.dto.response.*
import com.otoki.powersales.auth.exception.InvalidCredentialsException
import com.otoki.powersales.auth.exception.InvalidCurrentPasswordException
import com.otoki.powersales.auth.exception.InvalidTokenException
import com.otoki.powersales.auth.exception.NewPasswordPolicyViolationException
import com.otoki.powersales.auth.exception.NewPasswordSameAsTemporaryException
import com.otoki.powersales.auth.exception.TermsNotFoundException
import com.otoki.powersales.auth.exception.TokenReuseDetectedException
import com.otoki.powersales.common.security.PasswordChangeRequiredFilter
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.common.test.MobileControllerTestSupport
import com.otoki.powersales.auth.service.AuthService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest : MobileControllerTestSupport() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var authService: AuthService

    @MockkBean
    private lateinit var passwordChangeRequiredFilter: PasswordChangeRequiredFilter

    // ========== Login Tests ==========

    @Test
    @DisplayName("정상 로그인 - 200 OK, 사용자 정보 및 토큰 반환")
    fun login_success() {
        val request = LoginRequest(employeeCode = "12345678", password = "password123")
        val mockResponse = LoginResponse(
            user = UserInfo(1L, "12345678", "홍길동", "서울지점", "WOMAN", "여사원"),
            token = TokenInfo("access-token", "refresh-token", 3600),
            passwordChangeRequired = false,
            requiresGpsConsent = false
        )

        every { authService.login(any()) } returns mockResponse

        mockMvc.perform(
            post("/api/v1/mobile/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.user.id").value(1))
            .andExpect(jsonPath("$.data.token.accessToken").value("access-token"))
            .andExpect(jsonPath("$.data.passwordChangeRequired").value(false))
    }

    @Test
    @DisplayName("초기 비밀번호로 로그인 - 200 OK, password_change_required=true")
    fun login_initialPassword() {
        val mockResponse = LoginResponse(
            user = UserInfo(2L, "87654321", "김철수", "부산지점", "WOMAN", "여사원"),
            token = TokenInfo("access-token", "refresh-token", 3600),
            passwordChangeRequired = true,
            requiresGpsConsent = true
        )

        every { authService.login(any()) } returns mockResponse

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
        every { authService.login(any()) } throws InvalidCredentialsException()

        mockMvc.perform(
            post("/api/v1/mobile/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"employeeCode": "99999999", "password": "password123"}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
    }

    @Test
    @DisplayName("필수 필드 누락 (employeeCode 빈 값) - 400 INVALID_PARAMETER")
    fun login_missingEmployeeCode() {
        mockMvc.perform(
            post("/api/v1/mobile/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"employeeCode": "", "password": "password123"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
    }

    @Test
    @DisplayName("사번 형식 오류 (6자리) - 400 with validation message")
    fun login_invalidEmployeeCodeFormat() {
        mockMvc.perform(
            post("/api/v1/mobile/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"employeeCode": "123456", "password": "password123"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
            .andExpect(jsonPath("$.error.message", containsString("사번은 8자리 숫자 또는 'ADMIN-'")))
    }

    @Test
    @DisplayName("비밀번호 길이 부족 - 400 with validation message")
    fun login_shortPassword() {
        mockMvc.perform(
            post("/api/v1/mobile/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"employeeCode": "12345678", "password": "123"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
            .andExpect(jsonPath("$.error.message", containsString("4글자 이상")))
    }

    // ========== Token Refresh Tests ==========

    @Test
    @DisplayName("토큰 갱신 성공 - 200 OK, access_token + refresh_token 반환")
    fun refresh_success() {
        val mockResponse = TokenResponse(
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token",
            expiresIn = 3600
        )

        every { authService.refreshAccessToken(any()) } returns mockResponse

        mockMvc.perform(
            post("/api/v1/mobile/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken": "valid-refresh-token"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("com.otoki.powersales.auth.controller.AuthControllerTest#refreshExceptions")
    @DisplayName("Refresh 토큰 - 예외 → 401 ErrorCode 매핑")
    fun refresh_exceptions(
        @Suppress("UNUSED_PARAMETER") name: String,
        exception: Throwable,
        expectedCode: String,
    ) {
        every { authService.refreshAccessToken(any()) } throws exception

        mockMvc.perform(
            post("/api/v1/mobile/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken": "bad-token"}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value(expectedCode))
    }

    @Test
    @DisplayName("Refresh Token 필수 필드 누락 - 400 INVALID_PARAMETER")
    fun refresh_missingToken() {
        mockMvc.perform(
            post("/api/v1/mobile/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken": ""}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
    }

    // ========== Change Password Tests ==========

    @Test
    @DisplayName("비밀번호 변경 성공 - 200 OK + 새 토큰")
    fun changePassword_success() {
        val mockResponse = ChangePasswordResponse("new-access", "new-refresh", 3600)
        every { authService.changePassword(any<UserPrincipal>(), any()) } returns mockResponse

        mockMvc.perform(
            post("/api/v1/mobile/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword": "oldPass", "newPassword": "newPass1"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").value("new-access"))
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("com.otoki.powersales.auth.controller.AuthControllerTest#changePasswordExceptions")
    @DisplayName("비밀번호 변경 - 예외 → ErrorCode 매핑")
    fun changePassword_exceptions(
        @Suppress("UNUSED_PARAMETER") name: String,
        exception: Throwable,
        expectedStatus: Int,
        expectedCode: String,
    ) {
        every { authService.changePassword(any<UserPrincipal>(), any()) } throws exception

        mockMvc.perform(
            post("/api/v1/mobile/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword": "wrongPass", "newPassword": "newPass1"}""")
        )
            .andExpect(status().`is`(expectedStatus))
            .andExpect(jsonPath("$.error.code").value(expectedCode))
    }

    @Test
    @DisplayName("비밀번호 변경 - 새 비밀번호 누락 시 400 INVALID_PARAMETER (validation)")
    fun changePassword_missingNewPassword() {
        mockMvc.perform(
            post("/api/v1/mobile/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword": "oldPass123", "newPassword": ""}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
    }

    @Test
    @DisplayName("비밀번호 변경 - 정책 위반 시 400 AUTH_NEW_PASSWORD_INVALID + violations 배열 (별도 - details 검증)")
    fun changePassword_policyViolation() {
        // controller 후처리: details.violations 배열 매핑 — 가드레일 5.3 으로 별도 검증
        every { authService.changePassword(any<UserPrincipal>(), any()) } throws
            NewPasswordPolicyViolationException(listOf("LENGTH_TOO_SHORT"))

        mockMvc.perform(
            post("/api/v1/mobile/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"newPassword": "abc"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("AUTH_NEW_PASSWORD_INVALID"))
            .andExpect(jsonPath("$.error.details.violations[0]").value("LENGTH_TOO_SHORT"))
    }

    // ========== Verify Password Tests ==========

    @Test
    @DisplayName("비밀번호 검증 성공 - 200 OK (인증 필요)")
    fun verifyPassword_success() {
        every { authService.verifyPassword(1L, any()) } just Runs

        mockMvc.perform(
            post("/api/v1/mobile/auth/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword": "correctPass123"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    @DisplayName("비밀번호 검증 실패 - 비밀번호 불일치 시 401 AUTH_CURRENT_PASSWORD_MISMATCH")
    fun verifyPassword_passwordMismatch() {
        every { authService.verifyPassword(1L, any()) } throws InvalidCurrentPasswordException()

        mockMvc.perform(
            post("/api/v1/mobile/auth/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"currentPassword": "wrongPass"}""")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error.code").value("AUTH_CURRENT_PASSWORD_MISMATCH"))
    }

    @Test
    @DisplayName("비밀번호 검증 - 비밀번호 필드 누락 시 400")
    fun verifyPassword_missingPassword() {
        mockMvc.perform(
            post("/api/v1/mobile/auth/verify-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"password": ""}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
    }

    // ========== Logout Tests ==========

    @Test
    @DisplayName("로그아웃 성공 - 204 No Content")
    fun logout_success() {
        val accessToken = "test-access-token"
        every { authService.logout(any()) } just Runs

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
        mockMvc.perform(
            post("/api/v1/mobile/auth/logout")
        )
            .andExpect(status().isNoContent)
    }

    // ========== GPS Consent Terms Tests ==========

    @Test
    @DisplayName("GPS 약관 조회 성공 - 200 OK")
    fun gpsConsentTerms_success() {
        val response = GpsConsentTermsResponse("AGR-2025-001", "약관 본문 텍스트")
        every { authService.getGpsConsentTerms() } returns response

        mockMvc.perform(get("/api/v1/mobile/auth/gps-consent/terms"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.agreementNumber").value("AGR-2025-001"))
    }

    @Test
    @DisplayName("GPS 약관 조회 실패 - 활성 약관 없음 404")
    fun gpsConsentTerms_notFound() {
        every { authService.getGpsConsentTerms() } throws TermsNotFoundException()

        mockMvc.perform(get("/api/v1/mobile/auth/gps-consent/terms"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error.code").value("TERMS_NOT_FOUND"))
    }

    // ========== GPS Consent Status Tests ==========

    @Test
    @DisplayName("GPS 동의 상태 조회 - 동의 필요 / 불필요 (boolean passthrough)")
    fun gpsConsentStatus_passthrough() {
        every { authService.getGpsConsentStatus(1L) } returns GpsConsentStatusResponse(requiresGpsConsent = true) andThen GpsConsentStatusResponse(requiresGpsConsent = false)

        mockMvc.perform(get("/api/v1/mobile/auth/gps-consent/status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.requiresGpsConsent").value(true))

        mockMvc.perform(get("/api/v1/mobile/auth/gps-consent/status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.requiresGpsConsent").value(false))
    }

    // ========== GPS Consent Record Tests ==========

    @Test
    @DisplayName("GPS 동의 기록 성공 - 약관번호 포함, 새 토큰 반환")
    fun gpsConsent_success_withAgreementNumber() {
        val response = GpsConsentRecordResponse(accessToken = "new-token", expiresIn = 3600)
        every { authService.recordGpsConsent(1L, any()) } returns response

        mockMvc.perform(
            post("/api/v1/mobile/auth/gps-consent")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"agreementNumber": "AGR-2025-001"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").value("new-token"))
    }

    @Test
    @DisplayName("GPS 동의 기록 성공 - 요청 본문 없이 (하위 호환)")
    fun gpsConsent_success_withoutBody() {
        val response = GpsConsentRecordResponse(accessToken = "new-token", expiresIn = 3600)
        every { authService.recordGpsConsent(1L, any()) } returns response

        mockMvc.perform(post("/api/v1/mobile/auth/gps-consent"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").value("new-token"))
    }

    companion object {
        @JvmStatic
        fun refreshExceptions(): List<Arguments> = listOf(
            Arguments.of(
                "tokenReuseDetected -> TOKEN_REUSE_DETECTED",
                TokenReuseDetectedException(),
                "TOKEN_REUSE_DETECTED",
            ),
            Arguments.of("invalidToken -> INVALID_TOKEN", InvalidTokenException(), "INVALID_TOKEN"),
        )

        @JvmStatic
        fun changePasswordExceptions(): List<Arguments> = listOf(
            Arguments.of(
                "wrongCurrentPassword -> 401 AUTH_CURRENT_PASSWORD_MISMATCH",
                InvalidCurrentPasswordException(),
                401,
                "AUTH_CURRENT_PASSWORD_MISMATCH",
            ),
            Arguments.of(
                "sameAsTemporary -> 400 AUTH_NEW_PASSWORD_SAME_AS_TEMP",
                NewPasswordSameAsTemporaryException(),
                400,
                "AUTH_NEW_PASSWORD_SAME_AS_TEMP",
            ),
        )
    }
}
