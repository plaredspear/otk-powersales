package com.otoki.powersales.auth.web.service

import com.otoki.powersales.auth.permission.SfPermissionResolver
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

@DisplayName("WebAuthenticationService 테스트")
class WebAuthenticationServiceTest {

    private val userRepository: UserRepository = mockk()
    private val webUserDetailsService: WebUserDetailsService = mockk()
    private val webJwtService: WebJwtService = mockk()
    private val webRefreshTokenStore: WebRefreshTokenStore = mockk(relaxUnitFun = true)
    private val passwordEncoder: PasswordEncoder = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val sfPermissionResolver: SfPermissionResolver = mockk()
    private val passwordPolicyValidator: PasswordPolicyValidator = PasswordPolicyValidator()

    private val service = WebAuthenticationService(
        userRepository,
        webUserDetailsService,
        webJwtService,
        webRefreshTokenStore,
        passwordEncoder,
        passwordPolicyValidator,
        employeeRepository,
        sfPermissionResolver,
    )

    @org.junit.jupiter.api.BeforeEach
    fun stubSfPermissionResolver() {
        every { sfPermissionResolver.resolveForUser(any()) } returns emptySet()
    }

    @Nested
    @DisplayName("login - Web 로그인")
    inner class LoginTests {

        @Test
        @DisplayName("성공 - 유효한 username + password → access/refresh token 발급 + last_login_at 갱신")
        fun login_success() {
            val user = createUser()
            every { userRepository.findByUsername("u@otokims.co.kr") } returns user
            every { passwordEncoder.matches("password123", user.password) } returns true
            every { employeeRepository.findByEmployeeCode(any()) } returns Optional.empty()
            every { sfPermissionResolver.resolveForUser(any()) } returns emptySet()
            every { webJwtService.createAccessToken(any(), any(), any()) } returns "access-token"
            every { webJwtService.createRefreshToken(any(), any(), any(), any()) } returns "refresh-token"
            every { webJwtService.getAccessTokenExpirationSeconds() } returns 3600
            every { webJwtService.getRefreshExpirationMillis() } returns 7 * 24 * 60 * 60 * 1000L

            val response = service.login(WebLoginRequest("u@otokims.co.kr", "password123"))

            assertThat(response.accessToken).isEqualTo("access-token")
            assertThat(response.refreshToken).isEqualTo("refresh-token")
            assertThat(response.expiresIn).isEqualTo(3600)
            assertThat(response.passwordChangeRequired).isFalse()
            assertThat(response.user.username).isEqualTo("u@otokims.co.kr")
            assertThat(response.user.profileType).isEqualTo(ProfileType.STAFF)
            assertThat(user.lastLoginAt).isNotNull
            verify { webRefreshTokenStore.store(any(), 1L, any(), any()) }
        }

        @Test
        @DisplayName("실패 - User 미존재 → INVALID_CREDENTIALS")
        fun login_userNotFound() {
            every { userRepository.findByUsername("missing@otokims.co.kr") } returns null

            assertThatThrownBy { service.login(WebLoginRequest("missing@otokims.co.kr", "p")) }
                .isInstanceOf(InvalidCredentialsException::class.java)
        }

        @Test
        @DisplayName("실패 - User.is_active=false → USER_INACTIVE")
        fun login_inactiveUser() {
            val user = createUser(isActive = false)
            every { userRepository.findByUsername("u@otokims.co.kr") } returns user

            assertThatThrownBy { service.login(WebLoginRequest("u@otokims.co.kr", "p")) }
                .isInstanceOf(UserInactiveException::class.java)
        }

        @Test
        @DisplayName("실패 - 비밀번호 불일치 → INVALID_CREDENTIALS")
        fun login_passwordMismatch() {
            val user = createUser()
            every { userRepository.findByUsername("u@otokims.co.kr") } returns user
            every { passwordEncoder.matches("wrong", user.password) } returns false

            assertThatThrownBy { service.login(WebLoginRequest("u@otokims.co.kr", "wrong")) }
                .isInstanceOf(InvalidCredentialsException::class.java)
        }

        @Test
        @DisplayName("성공 - password_change_required=true 인 사용자도 로그인 + 응답에 플래그 반영")
        fun login_passwordChangeRequired() {
            val user = createUser(passwordChangeRequired = true)
            every { userRepository.findByUsername("u@otokims.co.kr") } returns user
            every { passwordEncoder.matches("1234", user.password) } returns true
            every { employeeRepository.findByEmployeeCode(any()) } returns Optional.empty()
            every { webJwtService.createAccessToken(any(), any(), any()) } returns "a"
            every { webJwtService.createRefreshToken(any(), any(), any(), any()) } returns "r"
            every { webJwtService.getAccessTokenExpirationSeconds() } returns 3600
            every { webJwtService.getRefreshExpirationMillis() } returns 60_000L

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
            every { webJwtService.validateRefreshToken("rt") } returns true
            every { webJwtService.getFamilyIdFromToken("rt") } returns "fam-1"
            every { webJwtService.getTokenIdFromToken("rt") } returns "tok-1"
            every { webJwtService.getUserIdFromToken("rt") } returns 1L
            every { webRefreshTokenStore.isFamilyRevoked("fam-1") } returns false
            every { webRefreshTokenStore.exists("tok-1") } returns true
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { employeeRepository.findByEmployeeCode(any()) } returns Optional.empty()
            every { webJwtService.createAccessToken(any(), any(), any()) } returns "new-access"
            every { webJwtService.createRefreshToken(any(), any(), "fam-1", any()) } returns "new-refresh"
            every { webJwtService.getAccessTokenExpirationSeconds() } returns 3600
            every { webJwtService.getRefreshExpirationMillis() } returns 60_000L

            val response = service.refresh(WebRefreshTokenRequest("rt"))

            assertThat(response.accessToken).isEqualTo("new-access")
            assertThat(response.refreshToken).isEqualTo("new-refresh")
            verify { webRefreshTokenStore.delete("tok-1") }
            verify { webRefreshTokenStore.store(any(), 1L, "fam-1", any()) }
        }

        @Test
        @DisplayName("실패 - 서명 무효 / audience 불일치 → INVALID_TOKEN")
        fun refresh_invalidToken() {
            every { webJwtService.validateRefreshToken("rt") } returns false

            assertThatThrownBy { service.refresh(WebRefreshTokenRequest("rt")) }
                .isInstanceOf(InvalidTokenException::class.java)
        }

        @Test
        @DisplayName("실패 - family 무효화 상태 → TOKEN_REUSE_DETECTED")
        fun refresh_familyRevoked() {
            every { webJwtService.validateRefreshToken("rt") } returns true
            every { webJwtService.getFamilyIdFromToken("rt") } returns "fam-x"
            every { webJwtService.getTokenIdFromToken("rt") } returns "tok-x"
            every { webJwtService.getUserIdFromToken("rt") } returns 1L
            every { webRefreshTokenStore.isFamilyRevoked("fam-x") } returns true

            assertThatThrownBy { service.refresh(WebRefreshTokenRequest("rt")) }
                .isInstanceOf(TokenReuseDetectedException::class.java)
        }

        @Test
        @DisplayName("실패 - Redis 메타 부재 (재사용 감지) → family revoke + TOKEN_REUSE_DETECTED")
        fun refresh_reuseDetected() {
            every { webJwtService.validateRefreshToken("rt") } returns true
            every { webJwtService.getFamilyIdFromToken("rt") } returns "fam-y"
            every { webJwtService.getTokenIdFromToken("rt") } returns "tok-y"
            every { webJwtService.getUserIdFromToken("rt") } returns 1L
            every { webRefreshTokenStore.isFamilyRevoked("fam-y") } returns false
            every { webRefreshTokenStore.exists("tok-y") } returns false
            every { webJwtService.getRefreshExpirationMillis() } returns 60_000L

            assertThatThrownBy { service.refresh(WebRefreshTokenRequest("rt")) }
                .isInstanceOf(TokenReuseDetectedException::class.java)
            verify { webRefreshTokenStore.revokeFamily("fam-y", any()) }
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
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { passwordEncoder.matches("oldpw", user.password) } returns true
            every { passwordEncoder.encode("newpw123") } returns "encoded-new"

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
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { passwordEncoder.encode("newpw456") } returns "encoded-new-2"

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
            every { userRepository.findById(1L) } returns Optional.of(user)

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
            every { userRepository.findById(1L) } returns Optional.of(user)
            every { passwordEncoder.matches("wrong", user.password) } returns false

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

    private fun principalFor(user: User, passwordChangeRequired: Boolean = user.passwordChangeRequired ?: true): WebUserPrincipal =
        WebUserPrincipal(
            userId = user.id,
            usernameValue = user.username,
            employeeCode = user.employeeCode,
            employeeId = null,
            role = null,
            costCenterCode = null,
            profileType = user.profileType,
            isSalesSupport = user.isSalesSupport ?: false,
            passwordChangeRequired = passwordChangeRequired,
            permissions = emptySet(),
            encodedPassword = user.password,
            grantedAuthorities = emptyList(),
            active = user.isActive
        )
}
