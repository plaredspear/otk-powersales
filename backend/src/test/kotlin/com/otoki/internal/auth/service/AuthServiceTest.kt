package com.otoki.internal.auth.service

import com.otoki.internal.common.config.DeviceBindingProperties
import com.otoki.internal.auth.dto.request.ChangePasswordRequest
import com.otoki.internal.common.dto.request.GpsConsentRequest
import com.otoki.internal.auth.dto.request.LoginRequest
import com.otoki.internal.auth.dto.request.RefreshTokenRequest
import com.otoki.internal.auth.dto.request.VerifyPasswordRequest
import com.otoki.internal.common.entity.AgreementHistory
import com.otoki.internal.common.entity.AgreementWord
import com.otoki.internal.common.entity.LoginHistory
import com.otoki.internal.sap.entity.Employee
import com.otoki.internal.sap.entity.UserRole
import com.otoki.internal.auth.exception.*
import com.otoki.internal.common.exception.*
import com.otoki.internal.common.repository.AgreementHistoryRepository
import com.otoki.internal.common.repository.AgreementWordRepository
import com.otoki.internal.common.repository.LoginHistoryRepository
import com.otoki.internal.sap.repository.EmployeeRepository
import com.otoki.internal.common.security.JwtTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var loginHistoryRepository: LoginHistoryRepository

    @Mock
    private lateinit var agreementWordRepository: AgreementWordRepository

    @Mock
    private lateinit var agreementHistoryRepository: AgreementHistoryRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Mock
    private lateinit var deviceBindingProperties: DeviceBindingProperties

    @InjectMocks
    private lateinit var authService: AuthService

    @Captor
    private lateinit var employeeCaptor: ArgumentCaptor<Employee>

    // ========== Login Tests ==========

    @Test
    @DisplayName("로그인 성공 - 유효한 사번과 비밀번호로 로그인 시 LoginResponse 반환")
    fun login_success() {
        // Given
        val employeeNumber = "12345678"
        val rawPassword = "password123"
        val encodedPassword = "encoded_password"
        val employee = createTestEmployee(
            id = 1L,
            employeeNumber = employeeNumber,
            password = encodedPassword,
            passwordChangeRequired = true,
            agreementFlag = null
        )

        val loginRequest = LoginRequest(employeeNumber, rawPassword)
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_123"
        val expiresIn = 3600

        whenever(employeeRepository.findByEmployeeNumber(employeeNumber)).thenReturn(Optional.of(employee))
        whenever(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true)
        whenever(jwtTokenProvider.createAccessToken(employee.id, employee.role, false)).thenReturn(accessToken)
        whenever(jwtTokenProvider.createRefreshToken(eq(employee.id), any(), any())).thenReturn(refreshToken)
        whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(expiresIn)

        // When
        val response = authService.login(loginRequest)

        // Then
        assertThat(response.user.id).isEqualTo(1L)
        assertThat(response.user.employeeNumber).isEqualTo(employeeNumber)
        assertThat(response.user.name).isEqualTo("홍길동")
        assertThat(response.user.orgName).isEqualTo("서울지점")
        assertThat(response.user.role).isEqualTo("USER")
        assertThat(response.token.accessToken).isEqualTo(accessToken)
        assertThat(response.token.refreshToken).isEqualTo(refreshToken)
        assertThat(response.token.expiresIn).isEqualTo(expiresIn)
        assertThat(response.requiresPasswordChange).isTrue()
        assertThat(response.requiresGpsConsent).isTrue()
        verify(loginHistoryRepository).save(any<LoginHistory>())
        verify(jwtTokenProvider).storeRefreshToken(any(), eq(employee.id), any())
    }

    @Test
    @DisplayName("로그인 성공 시 이력 기록 - employeeId가 설정된 LoginHistory가 저장된다")
    fun login_success_savesLoginHistory() {
        // Given
        val employeeNumber = "12345678"
        val employee = createTestEmployee(id = 1L, employeeNumber = employeeNumber)
        val loginRequest = LoginRequest(employeeNumber, "password123")
        val historyCaptor = ArgumentCaptor.forClass(LoginHistory::class.java)

        whenever(employeeRepository.findByEmployeeNumber(employeeNumber)).thenReturn(Optional.of(employee))
        whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)
        whenever(jwtTokenProvider.createAccessToken(employee.id, employee.role)).thenReturn("token")
        whenever(jwtTokenProvider.createRefreshToken(eq(employee.id), any(), any())).thenReturn("refresh")
        whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600)

        // When
        authService.login(loginRequest)

        // Then
        verify(loginHistoryRepository).save(historyCaptor.capture())
        val savedHistory = historyCaptor.value
        assertThat(savedHistory.employeeId).isEqualTo(employeeNumber)
        assertThat(savedHistory.loginAt).isNotNull()
    }

    @Test
    @DisplayName("이력 기록 실패 시 로그인 정상 - DB 오류가 발생해도 LoginResponse를 정상 반환한다")
    fun login_historyFailure_stillReturnsResponse() {
        // Given
        val employeeNumber = "12345678"
        val employee = createTestEmployee(id = 1L, employeeNumber = employeeNumber)
        val loginRequest = LoginRequest(employeeNumber, "password123")

        whenever(employeeRepository.findByEmployeeNumber(employeeNumber)).thenReturn(Optional.of(employee))
        whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)
        whenever(loginHistoryRepository.save(any<LoginHistory>())).thenThrow(RuntimeException("DB error"))
        whenever(jwtTokenProvider.createAccessToken(employee.id, employee.role)).thenReturn("token")
        whenever(jwtTokenProvider.createRefreshToken(eq(employee.id), any(), any())).thenReturn("refresh")
        whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600)

        // When
        val response = authService.login(loginRequest)

        // Then
        assertThat(response.token.accessToken).isEqualTo("token")
        assertThat(response.user.employeeNumber).isEqualTo(employeeNumber)
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 사번으로 로그인 시 InvalidCredentialsException 발생")
    fun login_userNotFound() {
        // Given
        val loginRequest = LoginRequest("99999999", "password123")

        whenever(employeeRepository.findByEmployeeNumber("99999999")).thenReturn(Optional.empty())

        // When & Then
        assertThatThrownBy { authService.login(loginRequest) }
            .isInstanceOf(InvalidCredentialsException::class.java)
        verify(loginHistoryRepository, never()).save(any<LoginHistory>())
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치 시 InvalidCredentialsException 발생")
    fun login_passwordMismatch() {
        // Given
        val employee = createTestEmployee(password = "encoded_password")
        val loginRequest = LoginRequest("12345678", "wrong_password")

        whenever(employeeRepository.findByEmployeeNumber("12345678")).thenReturn(Optional.of(employee))
        whenever(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false)

        // When & Then
        assertThatThrownBy { authService.login(loginRequest) }
            .isInstanceOf(InvalidCredentialsException::class.java)
        verify(loginHistoryRepository, never()).save(any<LoginHistory>())
    }

    // ========== Change Password Tests ==========

    @Test
    @DisplayName("비밀번호 변경 성공 - 유효한 현재 비밀번호와 새 비밀번호로 변경")
    fun changePassword_success() {
        // Given
        val userId = 1L
        val employee = createTestEmployee(id = userId, password = "encoded_old", passwordChangeRequired = true)
        val request = ChangePasswordRequest("old_password", "new_pass")

        whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
        whenever(passwordEncoder.matches("old_password", "encoded_old")).thenReturn(true)
        whenever(passwordEncoder.encode("new_pass")).thenReturn("encoded_new")
        whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.arguments[0] }

        // When
        authService.changePassword(userId, request)

        // Then
        verify(employeeRepository).save(employeeCaptor.capture())
        val savedEmployee = employeeCaptor.value
        assertThat(savedEmployee.password).isEqualTo("encoded_new")
        assertThat(savedEmployee.passwordChangeRequired).isFalse()
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치 시 InvalidCurrentPasswordException 발생")
    fun changePassword_currentPasswordMismatch() {
        // Given
        val userId = 1L
        val employee = createTestEmployee(id = userId, password = "encoded_old")
        val request = ChangePasswordRequest("wrong_password", "new_pass")

        whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
        whenever(passwordEncoder.matches("wrong_password", "encoded_old")).thenReturn(false)

        // When & Then
        assertThatThrownBy { authService.changePassword(userId, request) }
            .isInstanceOf(InvalidCurrentPasswordException::class.java)
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 새 비밀번호가 4자 미만일 경우 InvalidPasswordFormatException 발생")
    fun changePassword_passwordTooShort() {
        // Given
        val userId = 1L
        val employee = createTestEmployee(id = userId, password = "encoded_old")
        val request = ChangePasswordRequest("old_password", "123")

        whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
        whenever(passwordEncoder.matches("old_password", "encoded_old")).thenReturn(true)

        // When & Then
        assertThatThrownBy { authService.changePassword(userId, request) }
            .isInstanceOf(InvalidPasswordFormatException::class.java)
            .hasMessageContaining("4글자 이상")
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 동일 문자 반복 비밀번호(1111) 사용 시 InvalidPasswordFormatException 발생")
    fun changePassword_repeatedCharacters() {
        // Given
        val userId = 1L
        val employee = createTestEmployee(id = userId, password = "encoded_old")
        val request = ChangePasswordRequest("old_password", "1111")

        whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
        whenever(passwordEncoder.matches("old_password", "encoded_old")).thenReturn(true)

        // When & Then
        assertThatThrownBy { authService.changePassword(userId, request) }
            .isInstanceOf(InvalidPasswordFormatException::class.java)
            .hasMessageContaining("동일한 비밀번호")
    }

    // ========== Refresh Token Rotation Tests ==========

    @Nested
    @DisplayName("refreshAccessToken - 토큰 갱신 (Rotation)")
    inner class RefreshAccessTokenTests {

        @Test
        @DisplayName("정상 Rotation - 유효한 Refresh Token으로 새 Access + Refresh Token 발급")
        fun refreshAccessToken_success() {
            // Given
            val userId = 1L
            val refreshToken = "valid_refresh_token"
            val newAccessToken = "new_access_token"
            val newRefreshToken = "new_refresh_token"
            val expiresIn = 3600
            val employee = createTestEmployee(id = userId)
            val request = RefreshTokenRequest(refreshToken)
            val tokenId = "token-id-123"
            val familyId = "family-id-456"

            whenever(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true)
            whenever(jwtTokenProvider.getTokenType(refreshToken)).thenReturn("refresh")
            whenever(jwtTokenProvider.getTokenIdFromToken(refreshToken)).thenReturn(tokenId)
            whenever(jwtTokenProvider.getFamilyIdFromToken(refreshToken)).thenReturn(familyId)
            whenever(jwtTokenProvider.isTokenFamilyRevoked(familyId)).thenReturn(false)
            whenever(jwtTokenProvider.isRefreshTokenStored(tokenId)).thenReturn(true)
            whenever(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId)
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(jwtTokenProvider.createAccessToken(userId, UserRole.USER, false)).thenReturn(newAccessToken)
            whenever(jwtTokenProvider.createRefreshToken(eq(userId), eq(familyId), any())).thenReturn(newRefreshToken)
            whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(expiresIn)

            // When
            val response = authService.refreshAccessToken(request)

            // Then
            assertThat(response.accessToken).isEqualTo(newAccessToken)
            assertThat(response.refreshToken).isEqualTo(newRefreshToken)
            assertThat(response.expiresIn).isEqualTo(expiresIn)
            verify(jwtTokenProvider).deleteRefreshToken(tokenId)
            verify(jwtTokenProvider).storeRefreshToken(any(), eq(userId), eq(familyId))
        }

        @Test
        @DisplayName("토큰 갱신 실패 - 유효하지 않은 Refresh Token 사용 시 InvalidTokenException 발생")
        fun refreshAccessToken_invalidToken() {
            // Given
            val request = RefreshTokenRequest("invalid_refresh_token")

            whenever(jwtTokenProvider.validateToken("invalid_refresh_token")).thenReturn(false)

            // When & Then
            assertThatThrownBy { authService.refreshAccessToken(request) }
                .isInstanceOf(InvalidTokenException::class.java)
        }

        @Test
        @DisplayName("토큰 갱신 실패 - Access Token을 Refresh Token으로 사용 시 InvalidTokenException 발생")
        fun refreshAccessToken_wrongTokenType() {
            // Given
            val accessToken = "access_token_not_refresh"
            val request = RefreshTokenRequest(accessToken)

            whenever(jwtTokenProvider.validateToken(accessToken)).thenReturn(true)
            whenever(jwtTokenProvider.getTokenType(accessToken)).thenReturn("access")

            // When & Then
            assertThatThrownBy { authService.refreshAccessToken(request) }
                .isInstanceOf(InvalidTokenException::class.java)
        }

        @Test
        @DisplayName("탈취 감지 - Redis에 없는 토큰 사용 시 Family 무효화 + TokenReuseDetectedException")
        fun refreshAccessToken_tokenReuseDetected() {
            // Given
            val refreshToken = "reused_refresh_token"
            val request = RefreshTokenRequest(refreshToken)
            val tokenId = "already-used-token-id"
            val familyId = "family-id-to-revoke"

            whenever(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true)
            whenever(jwtTokenProvider.getTokenType(refreshToken)).thenReturn("refresh")
            whenever(jwtTokenProvider.getTokenIdFromToken(refreshToken)).thenReturn(tokenId)
            whenever(jwtTokenProvider.getFamilyIdFromToken(refreshToken)).thenReturn(familyId)
            whenever(jwtTokenProvider.isTokenFamilyRevoked(familyId)).thenReturn(false)
            whenever(jwtTokenProvider.isRefreshTokenStored(tokenId)).thenReturn(false)

            // When & Then
            assertThatThrownBy { authService.refreshAccessToken(request) }
                .isInstanceOf(TokenReuseDetectedException::class.java)
            verify(jwtTokenProvider).revokeTokenFamily(familyId)
        }

        @Test
        @DisplayName("무효화된 Family - 이미 revoked된 Family의 토큰 사용 시 TokenReuseDetectedException")
        fun refreshAccessToken_revokedFamily() {
            // Given
            val refreshToken = "family_revoked_token"
            val request = RefreshTokenRequest(refreshToken)
            val tokenId = "some-token-id"
            val familyId = "revoked-family-id"

            whenever(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true)
            whenever(jwtTokenProvider.getTokenType(refreshToken)).thenReturn("refresh")
            whenever(jwtTokenProvider.getTokenIdFromToken(refreshToken)).thenReturn(tokenId)
            whenever(jwtTokenProvider.getFamilyIdFromToken(refreshToken)).thenReturn(familyId)
            whenever(jwtTokenProvider.isTokenFamilyRevoked(familyId)).thenReturn(true)

            // When & Then
            assertThatThrownBy { authService.refreshAccessToken(request) }
                .isInstanceOf(TokenReuseDetectedException::class.java)
        }
    }

    // ========== Verify Password Tests ==========

    @Test
    @DisplayName("비밀번호 검증 성공 - 올바른 비밀번호로 검증 시 예외 없이 정상 완료")
    fun verifyPassword_success() {
        // Given
        val userId = 1L
        val employee = createTestEmployee(id = userId, password = "encoded_password")
        val request = VerifyPasswordRequest("correct_password")

        whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
        whenever(passwordEncoder.matches("correct_password", "encoded_password")).thenReturn(true)

        // When & Then (예외 없이 정상 완료)
        authService.verifyPassword(userId, request)
    }

    @Test
    @DisplayName("비밀번호 검증 실패 - 잘못된 비밀번호로 검증 시 InvalidCurrentPasswordException 발생")
    fun verifyPassword_passwordMismatch() {
        // Given
        val userId = 1L
        val employee = createTestEmployee(id = userId, password = "encoded_password")
        val request = VerifyPasswordRequest("wrong_password")

        whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
        whenever(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false)

        // When & Then
        assertThatThrownBy { authService.verifyPassword(userId, request) }
            .isInstanceOf(InvalidCurrentPasswordException::class.java)
    }

    @Test
    @DisplayName("비밀번호 검증 실패 - 존재하지 않는 사용자 ID로 요청 시 UserNotFoundException 발생")
    fun verifyPassword_userNotFound() {
        // Given
        val request = VerifyPasswordRequest("some_password")

        whenever(employeeRepository.findById(999L)).thenReturn(Optional.empty())

        // When & Then
        assertThatThrownBy { authService.verifyPassword(999L, request) }
            .isInstanceOf(EmployeeNotFoundException::class.java)
    }

    // ========== GPS Consent Tests ==========

    @Test
    @DisplayName("GPS 약관 조회 성공 - 활성 약관 반환")
    fun getGpsConsentTerms_success() {
        // Given
        val terms = AgreementWord(
            id = 1,
            name = "AGR-2025-001",
            contents = "약관 본문",
            active = true,
            isDeleted = false
        )
        whenever(agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse())
            .thenReturn(Optional.of(terms))

        // When
        val response = authService.getGpsConsentTerms()

        // Then
        assertThat(response.agreementNumber).isEqualTo("AGR-2025-001")
        assertThat(response.contents).isEqualTo("약관 본문")
    }

    @Test
    @DisplayName("GPS 약관 조회 실패 - 활성 약관 없음")
    fun getGpsConsentTerms_notFound() {
        // Given
        whenever(agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse())
            .thenReturn(Optional.empty())

        // When & Then
        assertThatThrownBy { authService.getGpsConsentTerms() }
            .isInstanceOf(TermsNotFoundException::class.java)
    }

    @Test
    @DisplayName("GPS 동의 상태 - 미동의 사용자")
    fun getGpsConsentStatus_requiresConsent() {
        // Given
        val employee = createTestEmployee(id = 1L, agreementFlag = null)
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))

        // When
        val response = authService.getGpsConsentStatus(1L)

        // Then
        assertThat(response.requiresGpsConsent).isTrue()
    }

    @Test
    @DisplayName("GPS 동의 상태 - 동의 완료 사용자")
    fun getGpsConsentStatus_consentGiven() {
        // Given
        val employee = createTestEmployee(id = 1L, agreementFlag = true)
        whenever(employeeRepository.findById(1L)).thenReturn(Optional.of(employee))

        // When
        val response = authService.getGpsConsentStatus(1L)

        // Then
        assertThat(response.requiresGpsConsent).isFalse()
    }

    @Nested
    @DisplayName("recordGpsConsent - GPS 동의 기록")
    inner class RecordGpsConsentTests {

        private val activeTerms = AgreementWord(
            id = 10, name = "AGR-2026-001", contents = "약관 본문", active = true, isDeleted = false
        )

        @Test
        @DisplayName("성공 - agreementNumber 미전달 시 활성 약관으로 이력 저장 + 토큰 반환")
        fun recordGpsConsent_success() {
            // Given
            val userId = 1L
            val employee = createTestEmployee(id = userId, agreementFlag = null)

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse())
                .thenReturn(Optional.of(activeTerms))
            whenever(agreementHistoryRepository.save(any<AgreementHistory>())).thenAnswer { it.arguments[0] }
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.arguments[0] }
            whenever(jwtTokenProvider.createAccessToken(userId, UserRole.USER, true)).thenReturn("new-token")
            whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600)

            // When
            val response = authService.recordGpsConsent(userId)

            // Then
            verify(employeeRepository).save(employeeCaptor.capture())
            val savedEmployee = employeeCaptor.value
            assertThat(savedEmployee.agreementFlag).isTrue()
            assertThat(savedEmployee.lastAgreementNumber).isEqualTo("AGR-2026-001")
            assertThat(response.accessToken).isEqualTo("new-token")
            assertThat(response.expiresIn).isEqualTo(3600)

            val historyCaptor = ArgumentCaptor.forClass(AgreementHistory::class.java)
            verify(agreementHistoryRepository).save(historyCaptor.capture())
            val history = historyCaptor.value
            assertThat(history.employeeId).isEqualTo(userId)
            assertThat(history.agreementFlag).isTrue()
            assertThat(history.agreementWordId).isEqualTo(10L)
        }

        @Test
        @DisplayName("성공 - 약관번호 지정 시 해당 약관으로 이력 저장")
        fun recordGpsConsent_withAgreementNumber() {
            // Given
            val userId = 1L
            val employee = createTestEmployee(id = userId, agreementFlag = null)
            val namedTerms = AgreementWord(
                id = 20, name = "AGR-CUSTOM", contents = "커스텀 약관", active = true, isDeleted = false
            )
            val request = GpsConsentRequest(agreementNumber = "AGR-CUSTOM")

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(agreementWordRepository.findByNameAndIsDeletedFalse("AGR-CUSTOM"))
                .thenReturn(Optional.of(namedTerms))
            whenever(agreementHistoryRepository.save(any<AgreementHistory>())).thenAnswer { it.arguments[0] }
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.arguments[0] }
            whenever(jwtTokenProvider.createAccessToken(userId, UserRole.USER, true)).thenReturn("new-token")
            whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600)

            // When
            authService.recordGpsConsent(userId, request)

            // Then
            verify(employeeRepository).save(employeeCaptor.capture())
            assertThat(employeeCaptor.value.lastAgreementNumber).isEqualTo("AGR-CUSTOM")

            val historyCaptor = ArgumentCaptor.forClass(AgreementHistory::class.java)
            verify(agreementHistoryRepository).save(historyCaptor.capture())
            assertThat(historyCaptor.value.agreementWordId).isEqualTo(20L)
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자 → UserNotFoundException")
        fun recordGpsConsent_userNotFound() {
            // Given
            whenever(employeeRepository.findById(999L)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { authService.recordGpsConsent(999L) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("실패 - 활성 약관 없음 → TermsNotFoundException")
        fun recordGpsConsent_noActiveTerms() {
            // Given
            val userId = 1L
            val employee = createTestEmployee(id = userId)
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse())
                .thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { authService.recordGpsConsent(userId) }
                .isInstanceOf(TermsNotFoundException::class.java)
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 약관번호 → TermsNotFoundException")
        fun recordGpsConsent_invalidAgreementNumber() {
            // Given
            val userId = 1L
            val employee = createTestEmployee(id = userId)
            val request = GpsConsentRequest(agreementNumber = "INVALID")

            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee))
            whenever(agreementWordRepository.findByNameAndIsDeletedFalse("INVALID"))
                .thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { authService.recordGpsConsent(userId, request) }
                .isInstanceOf(TermsNotFoundException::class.java)
        }
    }

    // ========== Logout Tests ==========

    @Test
    @DisplayName("로그아웃 성공 - Access Token 블랙리스트 + Refresh Token Redis 삭제")
    fun logout_success() {
        // Given
        val accessToken = "access_token_to_blacklist"
        val userId = 1L

        whenever(jwtTokenProvider.getUserIdFromToken(accessToken)).thenReturn(userId)

        // When
        authService.logout(accessToken)

        // Then
        verify(jwtTokenProvider).blacklistToken(accessToken)
        verify(jwtTokenProvider).deleteRefreshTokenByUserId(userId)
    }

    @Test
    @DisplayName("로그아웃 - 토큰 파싱 실패해도 블랙리스트 등록은 수행")
    fun logout_tokenParsingFails_stillBlacklists() {
        // Given
        val accessToken = "expired_or_invalid_token"

        whenever(jwtTokenProvider.getUserIdFromToken(accessToken)).thenThrow(RuntimeException("Token expired"))

        // When
        authService.logout(accessToken)

        // Then
        verify(jwtTokenProvider).blacklistToken(accessToken)
    }

    // ========== Device Binding Tests ==========

    @Test
    @DisplayName("최초 모바일 로그인 - device_id 전달 시 deviceUuid 저장 후 로그인 성공")
    fun login_firstMobileLogin_bindsDevice() {
        // Given
        val employee = createTestEmployee(id = 1L, deviceUuid = null)
        val request = LoginRequest("12345678", "password123", deviceId = "device-abc-123")

        whenever(employeeRepository.findByEmployeeNumber("12345678")).thenReturn(Optional.of(employee))
        whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)
        whenever(deviceBindingProperties.enabled).thenReturn(true)
        whenever(deviceBindingProperties.isExcluded("12345678")).thenReturn(false)
        whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.arguments[0] }
        whenever(jwtTokenProvider.createAccessToken(1L, UserRole.USER, false)).thenReturn("token")
        whenever(jwtTokenProvider.createRefreshToken(eq(1L), any(), any())).thenReturn("refresh")
        whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600)

        // When
        val response = authService.login(request)

        // Then
        assertThat(response.token.accessToken).isEqualTo("token")
        verify(employeeRepository).save(employeeCaptor.capture())
        assertThat(employeeCaptor.value.deviceUuid).isEqualTo("device-abc-123")
    }

    @Test
    @DisplayName("동일 단말기 로그인 - 등록된 device_id와 일치 시 로그인 성공")
    fun login_sameDevice_success() {
        // Given
        val employee = createTestEmployee(id = 1L, deviceUuid = "device-abc-123")
        val request = LoginRequest("12345678", "password123", deviceId = "device-abc-123")

        whenever(employeeRepository.findByEmployeeNumber("12345678")).thenReturn(Optional.of(employee))
        whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)
        whenever(deviceBindingProperties.enabled).thenReturn(true)
        whenever(deviceBindingProperties.isExcluded("12345678")).thenReturn(false)
        whenever(jwtTokenProvider.createAccessToken(1L, UserRole.USER, false)).thenReturn("token")
        whenever(jwtTokenProvider.createRefreshToken(eq(1L), any(), any())).thenReturn("refresh")
        whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600)

        // When
        val response = authService.login(request)

        // Then
        assertThat(response.token.accessToken).isEqualTo("token")
    }

    @Test
    @DisplayName("다른 단말기 로그인 - 등록된 device_id와 불일치 시 DeviceMismatchException 발생")
    fun login_differentDevice_throwsDeviceMismatch() {
        // Given
        val employee = createTestEmployee(id = 1L, deviceUuid = "device-abc-123")
        val request = LoginRequest("12345678", "password123", deviceId = "device-xyz-789")

        whenever(employeeRepository.findByEmployeeNumber("12345678")).thenReturn(Optional.of(employee))
        whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)
        whenever(deviceBindingProperties.enabled).thenReturn(true)
        whenever(deviceBindingProperties.isExcluded("12345678")).thenReturn(false)

        // When & Then
        assertThatThrownBy { authService.login(request) }
            .isInstanceOf(DeviceMismatchException::class.java)
    }

    @Test
    @DisplayName("웹 로그인 - device_id 미전달 시 단말기 검증 스킵, 로그인 성공")
    fun login_webLogin_skipsDeviceBinding() {
        // Given
        val employee = createTestEmployee(id = 1L, deviceUuid = "device-abc-123")
        val request = LoginRequest("12345678", "password123")

        whenever(employeeRepository.findByEmployeeNumber("12345678")).thenReturn(Optional.of(employee))
        whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)
        whenever(jwtTokenProvider.createAccessToken(1L, UserRole.USER, false)).thenReturn("token")
        whenever(jwtTokenProvider.createRefreshToken(eq(1L), any(), any())).thenReturn("refresh")
        whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600)

        // When
        val response = authService.login(request)

        // Then
        assertThat(response.token.accessToken).isEqualTo("token")
    }

    @Test
    @DisplayName("바인딩 비활성화 - enabled=false일 때 device_id 불일치여도 로그인 성공")
    fun login_bindingDisabled_skipsValidation() {
        // Given
        val employee = createTestEmployee(id = 1L, deviceUuid = "device-abc-123")
        val request = LoginRequest("12345678", "password123", deviceId = "device-xyz-789")

        whenever(employeeRepository.findByEmployeeNumber("12345678")).thenReturn(Optional.of(employee))
        whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)
        whenever(deviceBindingProperties.enabled).thenReturn(false)
        whenever(jwtTokenProvider.createAccessToken(1L, UserRole.USER, false)).thenReturn("token")
        whenever(jwtTokenProvider.createRefreshToken(eq(1L), any(), any())).thenReturn("refresh")
        whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600)

        // When
        val response = authService.login(request)

        // Then
        assertThat(response.token.accessToken).isEqualTo("token")
    }

    @Test
    @DisplayName("예외 사번 로그인 - excluded-ids에 포함된 사번은 device_id 불일치여도 로그인 성공")
    fun login_excludedEmployee_skipsValidation() {
        // Given
        val employee = createTestEmployee(id = 1L, employeeNumber = "20010585", deviceUuid = "device-abc-123")
        val request = LoginRequest("20010585", "password123", deviceId = "device-xyz-789")

        whenever(employeeRepository.findByEmployeeNumber("20010585")).thenReturn(Optional.of(employee))
        whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)
        whenever(deviceBindingProperties.enabled).thenReturn(true)
        whenever(deviceBindingProperties.isExcluded("20010585")).thenReturn(true)
        whenever(jwtTokenProvider.createAccessToken(1L, UserRole.USER, false)).thenReturn("token")
        whenever(jwtTokenProvider.createRefreshToken(eq(1L), any(), any())).thenReturn("refresh")
        whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600)

        // When
        val response = authService.login(request)

        // Then
        assertThat(response.token.accessToken).isEqualTo("token")
    }

    // ========== Login Authority Tests ==========

    @Nested
    @DisplayName("validateLoginAuthority - 로그인 권한 검증")
    inner class LoginAuthorityTests {

        @Test
        @DisplayName("WEB 로그인 성공 - 허용 권한(영업부장)으로 로그인 시 정상 반환")
        fun webLogin_allowedAuthority_success() {
            // Given
            val employee = createTestEmployee(id = 1L, appAuthority = "영업부장")
            val request = LoginRequest("12345678", "password123")

            whenever(employeeRepository.findByEmployeeNumber("12345678")).thenReturn(Optional.of(employee))
            whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)
            whenever(jwtTokenProvider.createAccessToken(1L, UserRole.USER, false)).thenReturn("token")
            whenever(jwtTokenProvider.createRefreshToken(eq(1L), any(), any())).thenReturn("refresh")
            whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600)

            // When
            val response = authService.login(request)

            // Then
            assertThat(response.token.accessToken).isEqualTo("token")
        }

        @Test
        @DisplayName("Mobile 로그인 성공 - appLoginActive=true로 로그인 시 정상 반환")
        fun mobileLogin_active_success() {
            // Given
            val employee = createTestEmployee(id = 1L, appLoginActive = true, deviceUuid = null)
            val request = LoginRequest("12345678", "password123", deviceId = "device-123")

            whenever(employeeRepository.findByEmployeeNumber("12345678")).thenReturn(Optional.of(employee))
            whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)
            whenever(deviceBindingProperties.enabled).thenReturn(true)
            whenever(deviceBindingProperties.isExcluded("12345678")).thenReturn(false)
            whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.arguments[0] }
            whenever(jwtTokenProvider.createAccessToken(1L, UserRole.USER, false)).thenReturn("token")
            whenever(jwtTokenProvider.createRefreshToken(eq(1L), any(), any())).thenReturn("refresh")
            whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600)

            // When
            val response = authService.login(request)

            // Then
            assertThat(response.token.accessToken).isEqualTo("token")
        }

        @Test
        @DisplayName("WEB 미허용 권한 - appAuthority가 허용 목록에 없으면 WebLoginNotAllowedException")
        fun webLogin_notAllowedAuthority_throws() {
            // Given
            val employee = createTestEmployee(id = 1L, appAuthority = "여사원")
            val request = LoginRequest("12345678", "password123")

            whenever(employeeRepository.findByEmployeeNumber("12345678")).thenReturn(Optional.of(employee))
            whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)

            // When & Then
            assertThatThrownBy { authService.login(request) }
                .isInstanceOf(WebLoginNotAllowedException::class.java)
        }

        @Test
        @DisplayName("WEB 권한 null - appAuthority가 null이면 WebLoginNotAllowedException")
        fun webLogin_nullAuthority_throws() {
            // Given
            val employee = createTestEmployee(id = 1L, appAuthority = null)
            val request = LoginRequest("12345678", "password123")

            whenever(employeeRepository.findByEmployeeNumber("12345678")).thenReturn(Optional.of(employee))
            whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)

            // When & Then
            assertThatThrownBy { authService.login(request) }
                .isInstanceOf(WebLoginNotAllowedException::class.java)
        }

        @Test
        @DisplayName("Mobile 비활성 - appLoginActive=false이면 AppLoginNotActiveException")
        fun mobileLogin_inactive_throws() {
            // Given
            val employee = createTestEmployee(id = 1L, appLoginActive = false)
            val request = LoginRequest("12345678", "password123", deviceId = "device-123")

            whenever(employeeRepository.findByEmployeeNumber("12345678")).thenReturn(Optional.of(employee))
            whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)

            // When & Then
            assertThatThrownBy { authService.login(request) }
                .isInstanceOf(AppLoginNotActiveException::class.java)
        }

        @Test
        @DisplayName("Mobile null - appLoginActive=null이면 AppLoginNotActiveException")
        fun mobileLogin_nullActive_throws() {
            // Given
            val employee = createTestEmployee(id = 1L, appLoginActive = null)
            val request = LoginRequest("12345678", "password123", deviceId = "device-123")

            whenever(employeeRepository.findByEmployeeNumber("12345678")).thenReturn(Optional.of(employee))
            whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)

            // When & Then
            assertThatThrownBy { authService.login(request) }
                .isInstanceOf(AppLoginNotActiveException::class.java)
        }
    }

    // ========== Admin Login Tests ==========

    @Nested
    @DisplayName("adminLogin - 관리자 로그인")
    inner class AdminLoginTests {

        @Test
        @DisplayName("성공 - 허용 권한(조장)으로 관리자 로그인 시 AdminLoginResponse 반환")
        fun adminLogin_success() {
            // Given
            val employee = createTestEmployee(id = 1L, appAuthority = "조장", costCenterCode = "CC001")
            val request = LoginRequest("12345678", "password123")

            whenever(employeeRepository.findByEmployeeNumber("12345678")).thenReturn(Optional.of(employee))
            whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)
            whenever(jwtTokenProvider.createAccessToken(1L, UserRole.LEADER, false)).thenReturn("admin-token")
            whenever(jwtTokenProvider.createRefreshToken(eq(1L), any(), any())).thenReturn("admin-refresh")
            whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600)

            // When
            val response = authService.adminLogin(request)

            // Then
            assertThat(response.user.id).isEqualTo(1L)
            assertThat(response.user.employeeNumber).isEqualTo("12345678")
            assertThat(response.user.appAuthority).isEqualTo("조장")
            assertThat(response.user.costCenterCode).isEqualTo("CC001")
            assertThat(response.user.role).isEqualTo("LEADER")
            assertThat(response.token.accessToken).isEqualTo("admin-token")
            assertThat(response.token.refreshToken).isEqualTo("admin-refresh")
            assertThat(response.token.expiresIn).isEqualTo(3600)
            verify(loginHistoryRepository).save(any<LoginHistory>())
            verify(jwtTokenProvider).storeRefreshToken(any(), eq(1L), any())
        }

        @Test
        @DisplayName("실패 - 사번 불일치 시 InvalidCredentialsException")
        fun adminLogin_userNotFound() {
            // Given
            val request = LoginRequest("99999999", "password123")
            whenever(employeeRepository.findByEmployeeNumber("99999999")).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { authService.adminLogin(request) }
                .isInstanceOf(InvalidCredentialsException::class.java)
        }

        @Test
        @DisplayName("실패 - 비밀번호 불일치 시 InvalidCredentialsException")
        fun adminLogin_passwordMismatch() {
            // Given
            val employee = createTestEmployee(id = 1L, appAuthority = "조장")
            val request = LoginRequest("12345678", "wrong")

            whenever(employeeRepository.findByEmployeeNumber("12345678")).thenReturn(Optional.of(employee))
            whenever(passwordEncoder.matches("wrong", "encoded_password")).thenReturn(false)

            // When & Then
            assertThatThrownBy { authService.adminLogin(request) }
                .isInstanceOf(InvalidCredentialsException::class.java)
        }

        @Test
        @DisplayName("실패 - appAuthority가 허용 목록에 없으면 WebLoginNotAllowedException")
        fun adminLogin_notAllowed() {
            // Given
            val employee = createTestEmployee(id = 1L, appAuthority = "여사원")
            val request = LoginRequest("12345678", "password123")

            whenever(employeeRepository.findByEmployeeNumber("12345678")).thenReturn(Optional.of(employee))
            whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)

            // When & Then
            assertThatThrownBy { authService.adminLogin(request) }
                .isInstanceOf(WebLoginNotAllowedException::class.java)
        }

        @Test
        @DisplayName("실패 - appAuthority가 null이면 WebLoginNotAllowedException")
        fun adminLogin_nullAuthority() {
            // Given
            val employee = createTestEmployee(id = 1L, appAuthority = null)
            val request = LoginRequest("12345678", "password123")

            whenever(employeeRepository.findByEmployeeNumber("12345678")).thenReturn(Optional.of(employee))
            whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)

            // When & Then
            assertThatThrownBy { authService.adminLogin(request) }
                .isInstanceOf(WebLoginNotAllowedException::class.java)
        }
    }

    // ========== Reset Device Tests ==========

    @Test
    @DisplayName("단말기 초기화 성공 - 유효한 사번의 deviceUuid를 NULL로 초기화")
    fun resetDevice_success() {
        // Given
        val employee = createTestEmployee(id = 1L, employeeNumber = "20010585", deviceUuid = "device-abc-123")

        whenever(employeeRepository.findByEmployeeNumber("20010585")).thenReturn(Optional.of(employee))
        whenever(employeeRepository.save(any<Employee>())).thenAnswer { it.arguments[0] }

        // When
        authService.resetDevice("20010585")

        // Then
        verify(employeeRepository).save(employeeCaptor.capture())
        assertThat(employeeCaptor.value.deviceUuid).isNull()
    }

    @Test
    @DisplayName("단말기 초기화 실패 - 존재하지 않는 사번 시 UserNotFoundException 발생")
    fun resetDevice_userNotFound() {
        // Given
        whenever(employeeRepository.findByEmployeeNumber("99999999")).thenReturn(Optional.empty())

        // When & Then
        assertThatThrownBy { authService.resetDevice("99999999") }
            .isInstanceOf(EmployeeNotFoundException::class.java)
    }

    // ========== Helper ==========

    private fun createTestEmployee(
        id: Long = 1L,
        employeeNumber: String = "12345678",
        password: String = "encoded_password",
        name: String = "홍길동",
        orgName: String = "서울지점",
        appAuthority: String? = "영업부장",
        appLoginActive: Boolean? = true,
        passwordChangeRequired: Boolean = true,
        agreementFlag: Boolean? = null,
        deviceUuid: String? = null,
        costCenterCode: String? = null
    ): Employee {
        return Employee(
            id = id,
            employeeNumber = employeeNumber,
            password = password,
            name = name,
            orgName = orgName,
            appAuthority = appAuthority,
            appLoginActive = appLoginActive,
            passwordChangeRequired = passwordChangeRequired,
            agreementFlag = agreementFlag,
            deviceUuid = deviceUuid,
            costCenterCode = costCenterCode
        )
    }
}
