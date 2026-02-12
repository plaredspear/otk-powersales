package com.otoki.internal.service

import com.otoki.internal.dto.request.ChangePasswordRequest
import com.otoki.internal.dto.request.LoginRequest
import com.otoki.internal.dto.request.RefreshTokenRequest
import com.otoki.internal.dto.request.VerifyPasswordRequest
import com.otoki.internal.dto.response.LoginResponse
import com.otoki.internal.dto.response.TokenInfo
import com.otoki.internal.dto.response.TokenResponse
import com.otoki.internal.dto.response.UserInfo
import com.otoki.internal.exception.*
import com.otoki.internal.repository.UserRepository
import com.otoki.internal.security.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 인증 관련 비즈니스 로직
 */
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    /**
     * 로그인
     * 1. 사번으로 사용자 조회 (없으면 INVALID_CREDENTIALS)
     * 2. BCrypt 비밀번호 검증 (불일치 시 INVALID_CREDENTIALS)
     * 3. Access Token + Refresh Token 생성
     * 4. requiresPasswordChange, requiresGpsConsent 포함하여 반환
     */
    @Transactional(readOnly = true)
    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmployeeId(request.employeeId)
            .orElseThrow { InvalidCredentialsException() }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw InvalidCredentialsException()
        }

        val accessToken = jwtTokenProvider.createAccessToken(user.id, user.role)
        val refreshToken = jwtTokenProvider.createRefreshToken(user.id)

        return LoginResponse(
            user = UserInfo.from(user),
            token = TokenInfo(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds()
            ),
            requiresPasswordChange = user.passwordChangeRequired,
            requiresGpsConsent = user.requiresGpsConsent()
        )
    }

    /**
     * 비밀번호 변경
     * 1. userId로 사용자 조회
     * 2. 현재 비밀번호 BCrypt 검증
     * 3. 새 비밀번호 유효성 검증 (4자 이상, 동일 문자 반복 불가)
     * 4. BCrypt 암호화 저장, passwordChangeRequired = false
     */
    @Transactional
    fun changePassword(userId: Long, request: ChangePasswordRequest) {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        if (!passwordEncoder.matches(request.currentPassword, user.password)) {
            throw InvalidCurrentPasswordException()
        }

        validateNewPassword(request.newPassword)

        val encodedPassword = passwordEncoder.encode(request.newPassword)
        user.changePassword(encodedPassword)
        userRepository.save(user)
    }

    /**
     * 토큰 갱신
     * 1. Refresh Token 유효성 검증
     * 2. userId 추출 및 사용자 존재 확인
     * 3. 새 Access Token 생성
     */
    @Transactional(readOnly = true)
    fun refreshAccessToken(request: RefreshTokenRequest): TokenResponse {
        if (!jwtTokenProvider.validateToken(request.refreshToken)) {
            throw InvalidTokenException()
        }

        val tokenType = jwtTokenProvider.getTokenType(request.refreshToken)
        if (tokenType != "refresh") {
            throw InvalidTokenException()
        }

        val userId = jwtTokenProvider.getUserIdFromToken(request.refreshToken)
        val user = userRepository.findById(userId)
            .orElseThrow { InvalidTokenException() }

        val accessToken = jwtTokenProvider.createAccessToken(user.id, user.role)

        return TokenResponse(
            accessToken = accessToken,
            expiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds()
        )
    }

    /**
     * 로그아웃
     * Access Token을 블랙리스트에 추가
     */
    fun logout(accessToken: String) {
        jwtTokenProvider.blacklistToken(accessToken)
    }

    /**
     * 비밀번호 검증
     * 1. userId로 사용자 조회
     * 2. 현재 비밀번호 BCrypt 검증
     * 3. 검증 성공 시 정상 반환, 실패 시 InvalidCurrentPasswordException 발생
     */
    @Transactional(readOnly = true)
    fun verifyPassword(userId: Long, request: VerifyPasswordRequest) {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw InvalidCurrentPasswordException()
        }
    }

    /**
     * GPS 동의 기록
     */
    @Transactional
    fun recordGpsConsent(userId: Long) {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        user.recordGpsConsent()
        userRepository.save(user)
    }

    /**
     * 새 비밀번호 유효성 검증
     * - 4글자 미만 불가
     * - 모든 글자가 동일한 문자 불가 (예: 1111, aaaa)
     */
    private fun validateNewPassword(password: String) {
        if (password.length < 4) {
            throw InvalidPasswordFormatException("비밀번호는 4글자 이상이어야 합니다")
        }

        if (password.all { it == password[0] }) {
            throw InvalidPasswordFormatException("모든 글자가 동일한 비밀번호는 사용할 수 없습니다")
        }
    }
}
