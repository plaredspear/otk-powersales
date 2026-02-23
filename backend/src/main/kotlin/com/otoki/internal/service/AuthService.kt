package com.otoki.internal.service

import com.otoki.internal.config.DeviceBindingProperties
import com.otoki.internal.dto.request.ChangePasswordRequest
import com.otoki.internal.dto.request.LoginRequest
import com.otoki.internal.dto.request.RefreshTokenRequest
import com.otoki.internal.dto.request.VerifyPasswordRequest
import com.otoki.internal.dto.response.LoginResponse
import com.otoki.internal.dto.response.TokenInfo
import com.otoki.internal.dto.response.TokenResponse
import com.otoki.internal.dto.response.UserInfo
import com.otoki.internal.entity.LoginHistory
import com.otoki.internal.entity.User
import com.otoki.internal.exception.*
import com.otoki.internal.repository.LoginHistoryRepository
import com.otoki.internal.repository.UserRepository
import com.otoki.internal.security.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 인증 관련 비즈니스 로직
 */
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val loginHistoryRepository: LoginHistoryRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val deviceBindingProperties: DeviceBindingProperties
) {

    private val log = LoggerFactory.getLogger(AuthService::class.java)

    /**
     * 로그인
     * 1. 사번으로 사용자 조회 (없으면 INVALID_CREDENTIALS)
     * 2. BCrypt 비밀번호 검증 (불일치 시 INVALID_CREDENTIALS)
     * 3. 단말기 바인딩 검증 (device_id 존재 시)
     * 4. Access Token + Refresh Token 생성
     * 5. requiresPasswordChange, requiresGpsConsent 포함하여 반환
     */
    @Transactional
    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmployeeId(request.employeeId)
            .orElseThrow { InvalidCredentialsException() }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw InvalidCredentialsException()
        }

        // 단말기 바인딩 검증 (비밀번호 검증 이후)
        validateDeviceBinding(user, request.deviceId)

        // 로그인 이력 기록 (실패해도 로그인에 영향 없음)
        try {
            loginHistoryRepository.save(LoginHistory(employeeId = user.employeeId))
        } catch (e: Exception) {
            log.warn("로그인 이력 기록 실패: employeeId={}", user.employeeId, e)
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
            requiresPasswordChange = user.passwordChangeRequired ?: true,
            requiresGpsConsent = user.requiresGpsConsent()
        )
    }

    /**
     * 단말기 바인딩 검증
     * 1. device_id 미전달 → 검증 스킵 (웹 브라우저)
     * 2. 바인딩 비활성화 → 검증 스킵
     * 3. 예외 사번 → 검증 스킵
     * 4. DB deviceUuid == NULL → 최초 등록
     * 5. 일치 → 정상
     * 6. 불일치 → DEVICE_MISMATCH 에러
     */
    private fun validateDeviceBinding(user: User, deviceId: String?) {
        if (deviceId.isNullOrBlank()) return
        if (!deviceBindingProperties.enabled) return
        if (deviceBindingProperties.isExcluded(user.employeeId)) return

        if (user.deviceUuid == null) {
            user.bindDevice(deviceId)
            userRepository.save(user)
            return
        }

        if (user.deviceUuid != deviceId) {
            throw DeviceMismatchException()
        }
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
     * 단말기 초기화 (관리자 전용)
     */
    @Transactional
    fun resetDevice(employeeId: String) {
        val user = userRepository.findByEmployeeId(employeeId)
            .orElseThrow { UserNotFoundException() }

        user.resetDevice()
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
