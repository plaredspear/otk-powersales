package com.otoki.powersales.auth.web.service

import com.otoki.powersales.admin.service.AdminPermissionResolver
import com.otoki.powersales.auth.exception.CurrentPasswordRequiredException
import com.otoki.powersales.auth.exception.InvalidCredentialsException
import com.otoki.powersales.auth.exception.InvalidCurrentPasswordException
import com.otoki.powersales.auth.exception.InvalidTokenException
import com.otoki.powersales.auth.exception.TokenReuseDetectedException
import com.otoki.powersales.auth.exception.UserInactiveException
import com.otoki.powersales.auth.policy.PasswordPolicyValidator
import com.otoki.powersales.auth.web.WebJwtService
import com.otoki.powersales.auth.web.WebRefreshTokenStore
import com.otoki.powersales.auth.web.WebUserDetailsService
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.auth.web.dto.WebChangePasswordRequest
import com.otoki.powersales.auth.web.dto.WebLoginRequest
import com.otoki.powersales.auth.web.dto.WebRefreshTokenRequest
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("WebAuthenticationService 테스트")
class WebAuthenticationServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var webUserDetailsService: WebUserDetailsService

    @Mock
    private lateinit var webJwtService: WebJwtService

    @Mock
    private lateinit var webRefreshTokenStore: WebRefreshTokenStore

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var adminPermissionResolver: AdminPermissionResolver

    @org.mockito.Spy
    private val passwordPolicyValidator: PasswordPolicyValidator = PasswordPolicyValidator()

    @InjectMocks
    private lateinit var service: WebAuthenticationService

    @Nested
    @DisplayName("login - Web 로그인")
    inner class LoginTests {

        @Test
        @DisplayName("성공 - 유효한 username + password → access/refresh token 발급 + last_login_at 갱신")
        fun login_success() {
            val user = createUser()
            whenever(userRepository.findByUsername("u@otokims.co.kr")).thenReturn(user)
            whenever(passwordEncoder.matches("password123", user.password)).thenReturn(true)
            whenever(employeeRepository.findByEmployeeCode(any())).thenReturn(Optional.empty())
            whenever(webJwtService.createAccessToken(any(), anyOrNull(), any())).thenReturn("access-token")
            whenever(webJwtService.createRefreshToken(any(), any(), any(), any())).thenReturn("refresh-token")
            whenever(webJwtService.getAccessTokenExpirationSeconds()).thenReturn(3600)
            whenever(webJwtService.getRefreshExpirationMillis()).thenReturn(7 * 24 * 60 * 60 * 1000L)

            val response = service.login(WebLoginRequest("u@otokims.co.kr", "password123"))

            assertThat(response.accessToken).isEqualTo("access-token")
            assertThat(response.refreshToken).isEqualTo("refresh-token")
            assertThat(response.expiresIn).isEqualTo(3600)
            assertThat(response.passwordChangeRequired).isFalse()
            assertThat(response.user.username).isEqualTo("u@otokims.co.kr")
            assertThat(response.user.profileType).isEqualTo(ProfileType.STAFF)
            assertThat(user.lastLoginAt).isNotNull
            verify(webRefreshTokenStore).store(any(), eq(1L), any(), any())
        }

        @Test
        @DisplayName("실패 - User 미존재 → INVALID_CREDENTIALS")
        fun login_userNotFound() {
            whenever(userRepository.findByUsername("missing@otokims.co.kr")).thenReturn(null)

            assertThatThrownBy { service.login(WebLoginRequest("missing@otokims.co.kr", "p")) }
                .isInstanceOf(InvalidCredentialsException::class.java)
        }

        @Test
        @DisplayName("실패 - User.is_active=false → USER_INACTIVE")
        fun login_inactiveUser() {
            val user = createUser(isActive = false)
            whenever(userRepository.findByUsername("u@otokims.co.kr")).thenReturn(user)

            assertThatThrownBy { service.login(WebLoginRequest("u@otokims.co.kr", "p")) }
                .isInstanceOf(UserInactiveException::class.java)
        }

        @Test
        @DisplayName("실패 - 비밀번호 불일치 → INVALID_CREDENTIALS")
        fun login_passwordMismatch() {
            val user = createUser()
            whenever(userRepository.findByUsername("u@otokims.co.kr")).thenReturn(user)
            whenever(passwordEncoder.matches("wrong", user.password)).thenReturn(false)

            assertThatThrownBy { service.login(WebLoginRequest("u@otokims.co.kr", "wrong")) }
                .isInstanceOf(InvalidCredentialsException::class.java)
        }

        @Test
        @DisplayName("성공 - password_change_required=true 인 사용자도 로그인 + 응답에 플래그 반영")
        fun login_passwordChangeRequired() {
            val user = createUser(passwordChangeRequired = true)
            whenever(userRepository.findByUsername("u@otokims.co.kr")).thenReturn(user)
            whenever(passwordEncoder.matches("1234", user.password)).thenReturn(true)
            whenever(employeeRepository.findByEmployeeCode(any())).thenReturn(Optional.empty())
            whenever(webJwtService.createAccessToken(any(), anyOrNull(), any())).thenReturn("a")
            whenever(webJwtService.createRefreshToken(any(), any(), any(), any())).thenReturn("r")
            whenever(webJwtService.getAccessTokenExpirationSeconds()).thenReturn(3600)
            whenever(webJwtService.getRefreshExpirationMillis()).thenReturn(60_000L)

            val response = service.login(WebLoginRequest("u@otokims.co.kr", "1234"))

            assertThat(response.passwordChangeRequired).isTrue()
        }
    }

    @Nested
    @DisplayName("refresh - 토큰 갱신")
    inner class RefreshTests {

        @Test
        @DisplayName("성공 - 정상 refresh → 새 access/refresh token 발급 + 이전 token 삭제")
        fun refresh_success() {
            val user = createUser()
            whenever(webJwtService.validateRefreshToken("rt")).thenReturn(true)
            whenever(webJwtService.getFamilyIdFromToken("rt")).thenReturn("fam-1")
            whenever(webJwtService.getTokenIdFromToken("rt")).thenReturn("tok-1")
            whenever(webJwtService.getUserIdFromToken("rt")).thenReturn(1L)
            whenever(webRefreshTokenStore.isFamilyRevoked("fam-1")).thenReturn(false)
            whenever(webRefreshTokenStore.exists("tok-1")).thenReturn(true)
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
            whenever(employeeRepository.findByEmployeeCode(any())).thenReturn(Optional.empty())
            whenever(webJwtService.createAccessToken(any(), anyOrNull(), any())).thenReturn("new-access")
            whenever(webJwtService.createRefreshToken(any(), any(), eq("fam-1"), any())).thenReturn("new-refresh")
            whenever(webJwtService.getAccessTokenExpirationSeconds()).thenReturn(3600)
            whenever(webJwtService.getRefreshExpirationMillis()).thenReturn(60_000L)

            val response = service.refresh(WebRefreshTokenRequest("rt"))

            assertThat(response.accessToken).isEqualTo("new-access")
            assertThat(response.refreshToken).isEqualTo("new-refresh")
            verify(webRefreshTokenStore).delete("tok-1")
            verify(webRefreshTokenStore).store(any(), eq(1L), eq("fam-1"), any())
        }

        @Test
        @DisplayName("실패 - 서명 무효 / audience 불일치 → INVALID_TOKEN")
        fun refresh_invalidToken() {
            whenever(webJwtService.validateRefreshToken("rt")).thenReturn(false)

            assertThatThrownBy { service.refresh(WebRefreshTokenRequest("rt")) }
                .isInstanceOf(InvalidTokenException::class.java)
        }

        @Test
        @DisplayName("실패 - family 무효화 상태 → TOKEN_REUSE_DETECTED")
        fun refresh_familyRevoked() {
            whenever(webJwtService.validateRefreshToken("rt")).thenReturn(true)
            whenever(webJwtService.getFamilyIdFromToken("rt")).thenReturn("fam-x")
            whenever(webJwtService.getTokenIdFromToken("rt")).thenReturn("tok-x")
            whenever(webJwtService.getUserIdFromToken("rt")).thenReturn(1L)
            whenever(webRefreshTokenStore.isFamilyRevoked("fam-x")).thenReturn(true)

            assertThatThrownBy { service.refresh(WebRefreshTokenRequest("rt")) }
                .isInstanceOf(TokenReuseDetectedException::class.java)
        }

        @Test
        @DisplayName("실패 - Redis 메타 부재 (재사용 감지) → family revoke + TOKEN_REUSE_DETECTED")
        fun refresh_reuseDetected() {
            whenever(webJwtService.validateRefreshToken("rt")).thenReturn(true)
            whenever(webJwtService.getFamilyIdFromToken("rt")).thenReturn("fam-y")
            whenever(webJwtService.getTokenIdFromToken("rt")).thenReturn("tok-y")
            whenever(webJwtService.getUserIdFromToken("rt")).thenReturn(1L)
            whenever(webRefreshTokenStore.isFamilyRevoked("fam-y")).thenReturn(false)
            whenever(webRefreshTokenStore.exists("tok-y")).thenReturn(false)
            whenever(webJwtService.getRefreshExpirationMillis()).thenReturn(60_000L)

            assertThatThrownBy { service.refresh(WebRefreshTokenRequest("rt")) }
                .isInstanceOf(TokenReuseDetectedException::class.java)
            verify(webRefreshTokenStore).revokeFamily(eq("fam-y"), any())
        }
    }

    @Nested
    @DisplayName("changePassword - 비밀번호 변경")
    inner class ChangePasswordTests {

        @Test
        @DisplayName("성공 - 자발 변경 (currentPassword 일치) → 새 비밀번호 BCrypt 해시 + passwordChangeRequired=false")
        fun changePassword_voluntary_success() {
            val user = createUser(passwordChangeRequired = false)
            val principal = principalFor(user)
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
            whenever(passwordEncoder.matches("oldpw", user.password)).thenReturn(true)
            whenever(passwordEncoder.encode("newpw123")).thenReturn("encoded-new")

            val response = service.changePassword(
                principal,
                WebChangePasswordRequest(currentPassword = "oldpw", newPassword = "newpw123")
            )

            assertThat(response.passwordChangeRequired).isFalse()
            assertThat(user.password).isEqualTo("encoded-new")
            assertThat(user.passwordChangeRequired).isFalse()
        }

        @Test
        @DisplayName("성공 - 강제 변경 (passwordChangeRequired=true) → currentPassword 미검증")
        fun changePassword_forced_success() {
            val user = createUser(passwordChangeRequired = true)
            val principal = principalFor(user, passwordChangeRequired = true)
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
            whenever(passwordEncoder.encode("newpw456")).thenReturn("encoded-new-2")

            val response = service.changePassword(
                principal,
                WebChangePasswordRequest(currentPassword = null, newPassword = "newpw456")
            )

            assertThat(response.passwordChangeRequired).isFalse()
            assertThat(user.password).isEqualTo("encoded-new-2")
        }

        @Test
        @DisplayName("실패 - 자발 변경 + currentPassword 누락 → AUTH_CURRENT_PASSWORD_REQUIRED")
        fun changePassword_voluntary_missing() {
            val user = createUser(passwordChangeRequired = false)
            val principal = principalFor(user)
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))

            assertThatThrownBy {
                service.changePassword(
                    principal,
                    WebChangePasswordRequest(currentPassword = null, newPassword = "newpw123")
                )
            }.isInstanceOf(CurrentPasswordRequiredException::class.java)
        }

        @Test
        @DisplayName("실패 - 자발 변경 + currentPassword 불일치 → AUTH_CURRENT_PASSWORD_MISMATCH")
        fun changePassword_voluntary_mismatch() {
            val user = createUser(passwordChangeRequired = false)
            val principal = principalFor(user)
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
            whenever(passwordEncoder.matches("wrong", user.password)).thenReturn(false)

            assertThatThrownBy {
                service.changePassword(
                    principal,
                    WebChangePasswordRequest(currentPassword = "wrong", newPassword = "newpw123")
                )
            }.isInstanceOf(InvalidCurrentPasswordException::class.java)
        }
    }

    private fun createUser(
        id: Long = 1L,
        username: String = "u@otokims.co.kr",
        employeeCode: String = "S001",
        profileType: ProfileType = ProfileType.STAFF,
        isSalesSupport: Boolean = false,
        isActive: Boolean = true,
        passwordChangeRequired: Boolean = false
    ): User = User(
        id = id,
        username = username,
        isActive = isActive,
        employeeCode = employeeCode,
        profileType = profileType,
        isSalesSupport = isSalesSupport,
        password = "\$2a\$10\$encodedHash",
        passwordChangeRequired = passwordChangeRequired
    ).also { it.name = "홍길동" }

    private fun principalFor(user: User, passwordChangeRequired: Boolean = user.passwordChangeRequired): WebUserPrincipal =
        WebUserPrincipal(
            userId = user.id,
            usernameValue = user.username,
            employeeCode = user.employeeCode,
            employeeId = null,
            role = null,
            profileType = user.profileType,
            isSalesSupport = user.isSalesSupport,
            passwordChangeRequired = passwordChangeRequired,
            encodedPassword = user.password,
            grantedAuthorities = emptyList(),
            active = user.isActive
        )
}
