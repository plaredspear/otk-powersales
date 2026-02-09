package com.otoki.internal.security

import com.otoki.internal.entity.UserRole
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * JwtTokenProvider 테스트
 */
class JwtTokenProviderTest {

    private val jwtTokenProvider = JwtTokenProvider(
        secret = "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256-algorithm",
        accessExpiration = 3600000,  // 1 hour
        refreshExpiration = 604800000  // 7 days
    )

    @Test
    @DisplayName("createAccessToken은 유효한 JWT를 반환한다")
    fun createAccessToken_returnsValidJwt() {
        // Given
        val userId = 12345L
        val role = UserRole.USER

        // When
        val token = jwtTokenProvider.createAccessToken(userId, role)

        // Then
        assertNotNull(token)
        assertTrue(token.isNotEmpty())
        assertTrue(token.split(".").size == 3) // JWT has 3 parts: header.payload.signature
    }

    @Test
    @DisplayName("createRefreshToken은 유효한 JWT를 반환한다")
    fun createRefreshToken_returnsValidJwt() {
        // Given
        val userId = 12345L

        // When
        val token = jwtTokenProvider.createRefreshToken(userId)

        // Then
        assertNotNull(token)
        assertTrue(token.isNotEmpty())
        assertTrue(token.split(".").size == 3) // JWT has 3 parts: header.payload.signature
    }

    @Test
    @DisplayName("validateToken은 유효한 토큰에 대해 true를 반환한다")
    fun validateToken_returnsTrue_forValidToken() {
        // Given
        val userId = 12345L
        val role = UserRole.USER
        val token = jwtTokenProvider.createAccessToken(userId, role)

        // When
        val isValid = jwtTokenProvider.validateToken(token)

        // Then
        assertTrue(isValid)
    }

    @Test
    @DisplayName("validateToken은 만료된 토큰에 대해 false를 반환한다")
    fun validateToken_returnsFalse_forExpiredToken() {
        // Given: 만료 시간이 1ms인 provider 생성
        val shortLivedProvider = JwtTokenProvider(
            secret = "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256-algorithm",
            accessExpiration = 1,  // 1ms
            refreshExpiration = 1  // 1ms
        )
        val userId = 12345L
        val role = UserRole.USER
        val token = shortLivedProvider.createAccessToken(userId, role)

        // When: 토큰이 만료될 때까지 대기
        Thread.sleep(10)
        val isValid = shortLivedProvider.validateToken(token)

        // Then
        assertFalse(isValid)
    }

    @Test
    @DisplayName("validateToken은 블랙리스트에 추가된 토큰에 대해 false를 반환한다")
    fun validateToken_returnsFalse_forBlacklistedToken() {
        // Given
        val userId = 12345L
        val role = UserRole.USER
        val token = jwtTokenProvider.createAccessToken(userId, role)

        // When: 토큰을 블랙리스트에 추가
        jwtTokenProvider.blacklistToken(token)
        val isValid = jwtTokenProvider.validateToken(token)

        // Then
        assertFalse(isValid)
    }

    @Test
    @DisplayName("validateToken은 변조되거나 유효하지 않은 토큰에 대해 false를 반환한다")
    fun validateToken_returnsFalse_forTamperedToken() {
        // Given: 유효한 토큰 생성 후 일부 변조
        val userId = 12345L
        val role = UserRole.USER
        val validToken = jwtTokenProvider.createAccessToken(userId, role)
        val tamperedToken = validToken.dropLast(10) + "TAMPERED123"

        // When
        val isValid = jwtTokenProvider.validateToken(tamperedToken)

        // Then
        assertFalse(isValid)
    }

    @Test
    @DisplayName("validateToken은 완전히 잘못된 형식의 토큰에 대해 false를 반환한다")
    fun validateToken_returnsFalse_forInvalidFormatToken() {
        // Given
        val invalidToken = "this.is.not.a.valid.jwt.token"

        // When
        val isValid = jwtTokenProvider.validateToken(invalidToken)

        // Then
        assertFalse(isValid)
    }

    @Test
    @DisplayName("getUserIdFromToken은 올바른 userId를 추출한다")
    fun getUserIdFromToken_extractsCorrectUserId() {
        // Given
        val userId = 12345L
        val role = UserRole.USER
        val token = jwtTokenProvider.createAccessToken(userId, role)

        // When
        val extractedUserId = jwtTokenProvider.getUserIdFromToken(token)

        // Then
        assertEquals(userId, extractedUserId)
    }

    @Test
    @DisplayName("getRoleFromToken은 올바른 UserRole을 추출한다")
    fun getRoleFromToken_extractsCorrectRole() {
        // Given
        val userId = 12345L
        val role = UserRole.ADMIN
        val token = jwtTokenProvider.createAccessToken(userId, role)

        // When
        val extractedRole = jwtTokenProvider.getRoleFromToken(token)

        // Then
        assertEquals(role, extractedRole)
    }

    @Test
    @DisplayName("getRoleFromToken은 모든 UserRole 타입에 대해 올바르게 동작한다")
    fun getRoleFromToken_worksForAllRoleTypes() {
        // Given & When & Then: 모든 UserRole에 대해 테스트
        val userId = 12345L

        UserRole.values().forEach { role ->
            val token = jwtTokenProvider.createAccessToken(userId, role)
            val extractedRole = jwtTokenProvider.getRoleFromToken(token)
            assertEquals(role, extractedRole, "Failed for role: $role")
        }
    }

    @Test
    @DisplayName("getTokenType은 access token에 대해 'access'를 반환한다")
    fun getTokenType_returnsAccess_forAccessToken() {
        // Given
        val userId = 12345L
        val role = UserRole.USER
        val token = jwtTokenProvider.createAccessToken(userId, role)

        // When
        val tokenType = jwtTokenProvider.getTokenType(token)

        // Then
        assertEquals("access", tokenType)
    }

    @Test
    @DisplayName("getTokenType은 refresh token에 대해 'refresh'를 반환한다")
    fun getTokenType_returnsRefresh_forRefreshToken() {
        // Given
        val userId = 12345L
        val token = jwtTokenProvider.createRefreshToken(userId)

        // When
        val tokenType = jwtTokenProvider.getTokenType(token)

        // Then
        assertEquals("refresh", tokenType)
    }

    @Test
    @DisplayName("getAccessTokenExpirationSeconds는 올바른 값을 반환한다")
    fun getAccessTokenExpirationSeconds_returnsCorrectValue() {
        // Given: constructor에서 accessExpiration = 3600000 (1 hour = 3600 seconds)
        val expectedSeconds = 3600

        // When
        val actualSeconds = jwtTokenProvider.getAccessTokenExpirationSeconds()

        // Then
        assertEquals(expectedSeconds, actualSeconds)
    }

    @Test
    @DisplayName("blacklistToken은 토큰을 블랙리스트에 추가하고 검증 시 실패한다")
    fun blacklistToken_addsTokenToBlacklist() {
        // Given
        val userId = 12345L
        val role = UserRole.USER
        val token = jwtTokenProvider.createAccessToken(userId, role)

        // When: 처음에는 유효
        assertTrue(jwtTokenProvider.validateToken(token))

        // Then: 블랙리스트 추가 후 무효화
        jwtTokenProvider.blacklistToken(token)
        assertFalse(jwtTokenProvider.validateToken(token))
    }

    @Test
    @DisplayName("blacklistToken은 이미 만료된 토큰을 처리해도 예외가 발생하지 않는다")
    fun blacklistToken_handlesExpiredToken_withoutException() {
        // Given: 만료된 토큰 생성
        val shortLivedProvider = JwtTokenProvider(
            secret = "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256-algorithm",
            accessExpiration = 1,
            refreshExpiration = 1
        )
        val userId = 12345L
        val role = UserRole.USER
        val token = shortLivedProvider.createAccessToken(userId, role)
        Thread.sleep(10)

        // When & Then: 예외 없이 처리됨
        assertDoesNotThrow {
            shortLivedProvider.blacklistToken(token)
        }
    }

    @Test
    @DisplayName("서로 다른 userId로 생성된 토큰은 다른 userId를 반환한다")
    fun tokensWithDifferentUserIds_returnDifferentUserIds() {
        // Given
        val userId1 = 100L
        val userId2 = 200L
        val role = UserRole.USER

        // When
        val token1 = jwtTokenProvider.createAccessToken(userId1, role)
        val token2 = jwtTokenProvider.createAccessToken(userId2, role)

        // Then
        assertEquals(userId1, jwtTokenProvider.getUserIdFromToken(token1))
        assertEquals(userId2, jwtTokenProvider.getUserIdFromToken(token2))
        assertNotEquals(token1, token2)
    }

    @Test
    @DisplayName("동일한 사용자로 여러 번 생성된 토큰은 모두 유효하다")
    fun multipleTokensForSameUser_areAllValid() {
        // Given
        val userId = 12345L
        val role = UserRole.USER

        // When: 동일 사용자로 여러 토큰 생성
        val token1 = jwtTokenProvider.createAccessToken(userId, role)
        Thread.sleep(1000)  // 1초 대기하여 다른 iat(issued at) 보장
        val token2 = jwtTokenProvider.createAccessToken(userId, role)
        Thread.sleep(1000)
        val token3 = jwtTokenProvider.createAccessToken(userId, role)

        // Then: 모든 토큰이 유효
        assertTrue(jwtTokenProvider.validateToken(token1))
        assertTrue(jwtTokenProvider.validateToken(token2))
        assertTrue(jwtTokenProvider.validateToken(token3))

        // And: 모든 토큰이 동일한 userId 반환
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token1))
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token2))
        assertEquals(userId, jwtTokenProvider.getUserIdFromToken(token3))

        // And: 다른 시간에 생성되었으므로 토큰이 다름
        assertNotEquals(token1, token2)
        assertNotEquals(token2, token3)
        assertNotEquals(token1, token3)
    }

    @Test
    @DisplayName("refresh token은 role 정보를 포함하지 않는다")
    fun refreshToken_doesNotContainRole() {
        // Given
        val userId = 12345L
        val refreshToken = jwtTokenProvider.createRefreshToken(userId)

        // When & Then: refresh token에서 role을 추출하려 하면 예외 발생
        assertThrows<Exception> {
            jwtTokenProvider.getRoleFromToken(refreshToken)
        }
    }
}
