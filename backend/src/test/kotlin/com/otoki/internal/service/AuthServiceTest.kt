package com.otoki.internal.service

import com.otoki.internal.config.DeviceBindingProperties
import com.otoki.internal.dto.request.ChangePasswordRequest
import com.otoki.internal.dto.request.LoginRequest
import com.otoki.internal.dto.request.RefreshTokenRequest
import com.otoki.internal.dto.request.VerifyPasswordRequest
import com.otoki.internal.entity.LoginHistory
import com.otoki.internal.entity.User
import com.otoki.internal.entity.UserRole
import com.otoki.internal.exception.*
import com.otoki.internal.repository.LoginHistoryRepository
import com.otoki.internal.repository.UserRepository
import com.otoki.internal.security.JwtTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var loginHistoryRepository: LoginHistoryRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Mock
    private lateinit var deviceBindingProperties: DeviceBindingProperties

    @InjectMocks
    private lateinit var authService: AuthService

    @Captor
    private lateinit var userCaptor: ArgumentCaptor<User>

    // ========== Login Tests ==========

    @Test
    @DisplayName("로그인 성공 - 유효한 사번과 비밀번호로 로그인 시 LoginResponse 반환")
    fun login_success() {
        // Given
        val employeeId = "12345678"
        val rawPassword = "password123"
        val encodedPassword = "encoded_password"
        val user = createTestUser(
            id = 1L,
            employeeId = employeeId,
            password = encodedPassword,
            passwordChangeRequired = true,
            agreementFlag = null
        )

        val loginRequest = LoginRequest(employeeId, rawPassword)
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_123"
        val expiresIn = 3600

        whenever(userRepository.findByEmployeeId(employeeId)).thenReturn(Optional.of(user))
        whenever(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true)
        whenever(jwtTokenProvider.createAccessToken(user.id, user.role)).thenReturn(accessToken)
        whenever(jwtTokenProvider.createRefreshToken(user.id)).thenReturn(refreshToken)
        whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(expiresIn)

        // When
        val response = authService.login(loginRequest)

        // Then
        assertThat(response.user.id).isEqualTo(1L)
        assertThat(response.user.employeeId).isEqualTo(employeeId)
        assertThat(response.user.name).isEqualTo("홍길동")
        assertThat(response.user.orgName).isEqualTo("서울지점")
        assertThat(response.user.role).isEqualTo("USER")
        assertThat(response.token.accessToken).isEqualTo(accessToken)
        assertThat(response.token.refreshToken).isEqualTo(refreshToken)
        assertThat(response.token.expiresIn).isEqualTo(expiresIn)
        assertThat(response.requiresPasswordChange).isTrue()
        assertThat(response.requiresGpsConsent).isTrue()
        verify(loginHistoryRepository).save(any<LoginHistory>())
    }

    @Test
    @DisplayName("로그인 성공 시 이력 기록 - employeeId가 설정된 LoginHistory가 저장된다")
    fun login_success_savesLoginHistory() {
        // Given
        val employeeId = "12345678"
        val user = createTestUser(id = 1L, employeeId = employeeId)
        val loginRequest = LoginRequest(employeeId, "password123")
        val historyCaptor = ArgumentCaptor.forClass(LoginHistory::class.java)

        whenever(userRepository.findByEmployeeId(employeeId)).thenReturn(Optional.of(user))
        whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)
        whenever(jwtTokenProvider.createAccessToken(user.id, user.role)).thenReturn("token")
        whenever(jwtTokenProvider.createRefreshToken(user.id)).thenReturn("refresh")
        whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600)

        // When
        authService.login(loginRequest)

        // Then
        verify(loginHistoryRepository).save(historyCaptor.capture())
        val savedHistory = historyCaptor.value
        assertThat(savedHistory.employeeId).isEqualTo(employeeId)
        assertThat(savedHistory.loginAt).isNotNull()
    }

    @Test
    @DisplayName("이력 기록 실패 시 로그인 정상 - DB 오류가 발생해도 LoginResponse를 정상 반환한다")
    fun login_historyFailure_stillReturnsResponse() {
        // Given
        val employeeId = "12345678"
        val user = createTestUser(id = 1L, employeeId = employeeId)
        val loginRequest = LoginRequest(employeeId, "password123")

        whenever(userRepository.findByEmployeeId(employeeId)).thenReturn(Optional.of(user))
        whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)
        whenever(loginHistoryRepository.save(any<LoginHistory>())).thenThrow(RuntimeException("DB error"))
        whenever(jwtTokenProvider.createAccessToken(user.id, user.role)).thenReturn("token")
        whenever(jwtTokenProvider.createRefreshToken(user.id)).thenReturn("refresh")
        whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600)

        // When
        val response = authService.login(loginRequest)

        // Then
        assertThat(response.token.accessToken).isEqualTo("token")
        assertThat(response.user.employeeId).isEqualTo(employeeId)
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 사번으로 로그인 시 InvalidCredentialsException 발생")
    fun login_userNotFound() {
        // Given
        val loginRequest = LoginRequest("99999999", "password123")

        whenever(userRepository.findByEmployeeId("99999999")).thenReturn(Optional.empty())

        // When & Then
        assertThatThrownBy { authService.login(loginRequest) }
            .isInstanceOf(InvalidCredentialsException::class.java)
        verify(loginHistoryRepository, never()).save(any<LoginHistory>())
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치 시 InvalidCredentialsException 발생")
    fun login_passwordMismatch() {
        // Given
        val user = createTestUser(password = "encoded_password")
        val loginRequest = LoginRequest("12345678", "wrong_password")

        whenever(userRepository.findByEmployeeId("12345678")).thenReturn(Optional.of(user))
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
        val user = createTestUser(id = userId, password = "encoded_old", passwordChangeRequired = true)
        val request = ChangePasswordRequest("old_password", "new_pass")

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(passwordEncoder.matches("old_password", "encoded_old")).thenReturn(true)
        whenever(passwordEncoder.encode("new_pass")).thenReturn("encoded_new")
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }

        // When
        authService.changePassword(userId, request)

        // Then
        verify(userRepository).save(userCaptor.capture())
        val savedUser = userCaptor.value
        assertThat(savedUser.password).isEqualTo("encoded_new")
        assertThat(savedUser.passwordChangeRequired).isFalse()
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치 시 InvalidCurrentPasswordException 발생")
    fun changePassword_currentPasswordMismatch() {
        // Given
        val userId = 1L
        val user = createTestUser(id = userId, password = "encoded_old")
        val request = ChangePasswordRequest("wrong_password", "new_pass")

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
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
        val user = createTestUser(id = userId, password = "encoded_old")
        val request = ChangePasswordRequest("old_password", "123")

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
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
        val user = createTestUser(id = userId, password = "encoded_old")
        val request = ChangePasswordRequest("old_password", "1111")

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(passwordEncoder.matches("old_password", "encoded_old")).thenReturn(true)

        // When & Then
        assertThatThrownBy { authService.changePassword(userId, request) }
            .isInstanceOf(InvalidPasswordFormatException::class.java)
            .hasMessageContaining("동일한 비밀번호")
    }

    // ========== Refresh Token Tests ==========

    @Test
    @DisplayName("토큰 갱신 성공 - 유효한 Refresh Token으로 새 Access Token 발급")
    fun refreshAccessToken_success() {
        // Given
        val userId = 1L
        val refreshToken = "valid_refresh_token"
        val newAccessToken = "new_access_token"
        val expiresIn = 3600
        val user = createTestUser(id = userId)
        val request = RefreshTokenRequest(refreshToken)

        whenever(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true)
        whenever(jwtTokenProvider.getTokenType(refreshToken)).thenReturn("refresh")
        whenever(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId)
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(jwtTokenProvider.createAccessToken(userId, UserRole.USER)).thenReturn(newAccessToken)
        whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(expiresIn)

        // When
        val response = authService.refreshAccessToken(request)

        // Then
        assertThat(response.accessToken).isEqualTo(newAccessToken)
        assertThat(response.expiresIn).isEqualTo(expiresIn)
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

    // ========== Verify Password Tests ==========

    @Test
    @DisplayName("비밀번호 검증 성공 - 올바른 비밀번호로 검증 시 예외 없이 정상 완료")
    fun verifyPassword_success() {
        // Given
        val userId = 1L
        val user = createTestUser(id = userId, password = "encoded_password")
        val request = VerifyPasswordRequest("correct_password")

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(passwordEncoder.matches("correct_password", "encoded_password")).thenReturn(true)

        // When & Then (예외 없이 정상 완료)
        authService.verifyPassword(userId, request)
    }

    @Test
    @DisplayName("비밀번호 검증 실패 - 잘못된 비밀번호로 검증 시 InvalidCurrentPasswordException 발생")
    fun verifyPassword_passwordMismatch() {
        // Given
        val userId = 1L
        val user = createTestUser(id = userId, password = "encoded_password")
        val request = VerifyPasswordRequest("wrong_password")

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
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

        whenever(userRepository.findById(999L)).thenReturn(Optional.empty())

        // When & Then
        assertThatThrownBy { authService.verifyPassword(999L, request) }
            .isInstanceOf(UserNotFoundException::class.java)
    }

    // ========== GPS Consent Tests ==========

    @Test
    @DisplayName("GPS 동의 기록 성공 - agreementFlag가 true로 업데이트")
    fun recordGpsConsent_success() {
        // Given
        val userId = 1L
        val user = createTestUser(id = userId, agreementFlag = null)

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }

        // When
        authService.recordGpsConsent(userId)

        // Then
        verify(userRepository).save(userCaptor.capture())
        val savedUser = userCaptor.value
        assertThat(savedUser.agreementFlag).isTrue()
    }

    @Test
    @DisplayName("GPS 동의 기록 실패 - 존재하지 않는 사용자 ID로 요청 시 UserNotFoundException 발생")
    fun recordGpsConsent_userNotFound() {
        // Given
        whenever(userRepository.findById(999L)).thenReturn(Optional.empty())

        // When & Then
        assertThatThrownBy { authService.recordGpsConsent(999L) }
            .isInstanceOf(UserNotFoundException::class.java)
    }

    // ========== Logout Tests ==========

    @Test
    @DisplayName("로그아웃 성공 - Access Token을 블랙리스트에 추가")
    fun logout_success() {
        // Given
        val accessToken = "access_token_to_blacklist"

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
        val user = createTestUser(id = 1L, deviceUuid = null)
        val request = LoginRequest("12345678", "password123", deviceId = "device-abc-123")

        whenever(userRepository.findByEmployeeId("12345678")).thenReturn(Optional.of(user))
        whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)
        whenever(deviceBindingProperties.enabled).thenReturn(true)
        whenever(deviceBindingProperties.isExcluded("12345678")).thenReturn(false)
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }
        whenever(jwtTokenProvider.createAccessToken(1L, UserRole.USER)).thenReturn("token")
        whenever(jwtTokenProvider.createRefreshToken(1L)).thenReturn("refresh")
        whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600)

        // When
        val response = authService.login(request)

        // Then
        assertThat(response.token.accessToken).isEqualTo("token")
        verify(userRepository).save(userCaptor.capture())
        assertThat(userCaptor.value.deviceUuid).isEqualTo("device-abc-123")
    }

    @Test
    @DisplayName("동일 단말기 로그인 - 등록된 device_id와 일치 시 로그인 성공")
    fun login_sameDevice_success() {
        // Given
        val user = createTestUser(id = 1L, deviceUuid = "device-abc-123")
        val request = LoginRequest("12345678", "password123", deviceId = "device-abc-123")

        whenever(userRepository.findByEmployeeId("12345678")).thenReturn(Optional.of(user))
        whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)
        whenever(deviceBindingProperties.enabled).thenReturn(true)
        whenever(deviceBindingProperties.isExcluded("12345678")).thenReturn(false)
        whenever(jwtTokenProvider.createAccessToken(1L, UserRole.USER)).thenReturn("token")
        whenever(jwtTokenProvider.createRefreshToken(1L)).thenReturn("refresh")
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
        val user = createTestUser(id = 1L, deviceUuid = "device-abc-123")
        val request = LoginRequest("12345678", "password123", deviceId = "device-xyz-789")

        whenever(userRepository.findByEmployeeId("12345678")).thenReturn(Optional.of(user))
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
        val user = createTestUser(id = 1L, deviceUuid = "device-abc-123")
        val request = LoginRequest("12345678", "password123")

        whenever(userRepository.findByEmployeeId("12345678")).thenReturn(Optional.of(user))
        whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)
        whenever(jwtTokenProvider.createAccessToken(1L, UserRole.USER)).thenReturn("token")
        whenever(jwtTokenProvider.createRefreshToken(1L)).thenReturn("refresh")
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
        val user = createTestUser(id = 1L, deviceUuid = "device-abc-123")
        val request = LoginRequest("12345678", "password123", deviceId = "device-xyz-789")

        whenever(userRepository.findByEmployeeId("12345678")).thenReturn(Optional.of(user))
        whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)
        whenever(deviceBindingProperties.enabled).thenReturn(false)
        whenever(jwtTokenProvider.createAccessToken(1L, UserRole.USER)).thenReturn("token")
        whenever(jwtTokenProvider.createRefreshToken(1L)).thenReturn("refresh")
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
        val user = createTestUser(id = 1L, employeeId = "20010585", deviceUuid = "device-abc-123")
        val request = LoginRequest("20010585", "password123", deviceId = "device-xyz-789")

        whenever(userRepository.findByEmployeeId("20010585")).thenReturn(Optional.of(user))
        whenever(passwordEncoder.matches("password123", "encoded_password")).thenReturn(true)
        whenever(deviceBindingProperties.enabled).thenReturn(true)
        whenever(deviceBindingProperties.isExcluded("20010585")).thenReturn(true)
        whenever(jwtTokenProvider.createAccessToken(1L, UserRole.USER)).thenReturn("token")
        whenever(jwtTokenProvider.createRefreshToken(1L)).thenReturn("refresh")
        whenever(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600)

        // When
        val response = authService.login(request)

        // Then
        assertThat(response.token.accessToken).isEqualTo("token")
    }

    // ========== Reset Device Tests ==========

    @Test
    @DisplayName("단말기 초기화 성공 - 유효한 사번의 deviceUuid를 NULL로 초기화")
    fun resetDevice_success() {
        // Given
        val user = createTestUser(id = 1L, employeeId = "20010585", deviceUuid = "device-abc-123")

        whenever(userRepository.findByEmployeeId("20010585")).thenReturn(Optional.of(user))
        whenever(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }

        // When
        authService.resetDevice("20010585")

        // Then
        verify(userRepository).save(userCaptor.capture())
        assertThat(userCaptor.value.deviceUuid).isNull()
    }

    @Test
    @DisplayName("단말기 초기화 실패 - 존재하지 않는 사번 시 UserNotFoundException 발생")
    fun resetDevice_userNotFound() {
        // Given
        whenever(userRepository.findByEmployeeId("99999999")).thenReturn(Optional.empty())

        // When & Then
        assertThatThrownBy { authService.resetDevice("99999999") }
            .isInstanceOf(UserNotFoundException::class.java)
    }

    // ========== Helper ==========

    private fun createTestUser(
        id: Long = 1L,
        employeeId: String = "12345678",
        password: String = "encoded_password",
        name: String = "홍길동",
        orgName: String = "서울지점",
        appAuthority: String? = null,
        passwordChangeRequired: Boolean = true,
        agreementFlag: Boolean? = null,
        deviceUuid: String? = null
    ): User {
        return User(
            id = id,
            employeeId = employeeId,
            password = password,
            name = name,
            orgName = orgName,
            appAuthority = appAuthority,
            passwordChangeRequired = passwordChangeRequired,
            agreementFlag = agreementFlag,
            deviceUuid = deviceUuid
        )
    }
}
