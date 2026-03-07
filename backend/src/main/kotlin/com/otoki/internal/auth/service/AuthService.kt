package com.otoki.internal.auth.service

import com.otoki.internal.common.config.DeviceBindingProperties
import com.otoki.internal.auth.dto.request.ChangePasswordRequest
import com.otoki.internal.common.dto.request.GpsConsentRequest
import com.otoki.internal.auth.dto.request.LoginRequest
import com.otoki.internal.auth.dto.request.RefreshTokenRequest
import com.otoki.internal.auth.dto.request.VerifyPasswordRequest
import com.otoki.internal.auth.dto.response.*
import com.otoki.internal.common.dto.response.*
import com.otoki.internal.common.entity.AgreementHistory
import com.otoki.internal.common.entity.LoginHistory
import com.otoki.internal.sap.entity.User
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.auth.exception.*
import com.otoki.internal.common.exception.*
import com.otoki.internal.common.repository.AgreementHistoryRepository
import com.otoki.internal.common.repository.AgreementWordRepository
import com.otoki.internal.common.repository.LoginHistoryRepository
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.common.security.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

/**
 * 인증 관련 비즈니스 로직
 */
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val loginHistoryRepository: LoginHistoryRepository,
    private val agreementWordRepository: AgreementWordRepository,
    private val agreementHistoryRepository: AgreementHistoryRepository,
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
     * 4. Access Token + Refresh Token 생성 (familyId, tokenId 포함)
     * 5. Redis에 Refresh Token 메타데이터 저장
     * 6. requiresPasswordChange, requiresGpsConsent 포함하여 반환
     */
    @Transactional
    fun login(request: LoginRequest): LoginResponse {
        val user = userRepository.findByEmployeeId(request.employeeId)
            .orElseThrow { InvalidCredentialsException() }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw InvalidCredentialsException()
        }

        // 로그인 권한 검증 (비밀번호 검증 이후, 단말기 바인딩 이전)
        validateLoginAuthority(user, request.deviceId)

        // 단말기 바인딩 검증 (비밀번호 검증 이후)
        validateDeviceBinding(user, request.deviceId)

        // 로그인 이력 기록 (실패해도 로그인에 영향 없음)
        try {
            loginHistoryRepository.save(LoginHistory(employeeId = user.employeeId))
        } catch (e: Exception) {
            log.warn("로그인 이력 기록 실패: employeeId={}", user.employeeId, e)
        }

        val accessToken = jwtTokenProvider.createAccessToken(user.id, user.role, user.agreementFlag == true)

        // Refresh Token Rotation: familyId + tokenId 생성
        val familyId = UUID.randomUUID().toString()
        val tokenId = UUID.randomUUID().toString()
        val refreshToken = jwtTokenProvider.createRefreshToken(user.id, familyId, tokenId)

        // Redis에 Refresh Token 메타데이터 저장
        jwtTokenProvider.storeRefreshToken(tokenId, user.id, familyId)

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
     * 로그인 권한 검증
     * - WEB (deviceId 미전달): appAuthority가 허용 목록에 포함되어야 함
     * - Mobile (deviceId 전달): appLoginActive가 true여야 함
     */
    private fun validateLoginAuthority(user: User, deviceId: String?) {
        if (deviceId.isNullOrBlank()) {
            // WEB 로그인
            if (user.appAuthority == null || user.appAuthority !in AdminAuthorityFilter.ALLOWED_AUTHORITIES) {
                throw WebLoginNotAllowedException()
            }
        } else {
            // Mobile 로그인
            if (user.appLoginActive != true) {
                throw AppLoginNotActiveException()
            }
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
     * 토큰 갱신 (Refresh Token Rotation)
     * 1. Refresh Token JWT 서명/만료 검증
     * 2. JWT에서 tokenId, familyId 추출
     * 3. Token Family 무효화 여부 확인
     * 4. Redis에 Refresh Token 존재 여부 확인 (없으면 탈취 감지)
     * 5. 이전 Refresh Token Redis에서 삭제
     * 6. 새 tokenId 생성, 동일 familyId로 새 Refresh Token 발급
     * 7. Redis에 새 Refresh Token 저장
     * 8. 새 Access Token + 새 Refresh Token 반환
     */
    @Transactional(readOnly = true)
    fun refreshAccessToken(request: RefreshTokenRequest): TokenResponse {
        // 1. JWT 서명/만료 검증
        if (!jwtTokenProvider.validateToken(request.refreshToken)) {
            throw InvalidTokenException()
        }

        val tokenType = jwtTokenProvider.getTokenType(request.refreshToken)
        if (tokenType != "refresh") {
            throw InvalidTokenException()
        }

        // 2. JWT에서 tokenId, familyId 추출
        val tokenId = jwtTokenProvider.getTokenIdFromToken(request.refreshToken)
        val familyId = jwtTokenProvider.getFamilyIdFromToken(request.refreshToken)

        // 3. Token Family 무효화 여부 확인
        if (jwtTokenProvider.isTokenFamilyRevoked(familyId)) {
            throw TokenReuseDetectedException()
        }

        // 4. Redis에 Refresh Token 존재 여부 확인
        if (!jwtTokenProvider.isRefreshTokenStored(tokenId)) {
            // 이미 사용된 토큰 → 탈취 감지 → Family 전체 무효화
            jwtTokenProvider.revokeTokenFamily(familyId)
            throw TokenReuseDetectedException()
        }

        // 5. 이전 Refresh Token 삭제
        jwtTokenProvider.deleteRefreshToken(tokenId)

        // 6. 사용자 정보 조회
        val userId = jwtTokenProvider.getUserIdFromToken(request.refreshToken)
        val user = userRepository.findById(userId)
            .orElseThrow { InvalidTokenException() }

        // 7. 새 토큰 발급 (동일 familyId, 새 tokenId)
        val newTokenId = UUID.randomUUID().toString()
        val newAccessToken = jwtTokenProvider.createAccessToken(user.id, user.role, user.agreementFlag == true)
        val newRefreshToken = jwtTokenProvider.createRefreshToken(user.id, familyId, newTokenId)

        // 8. Redis에 새 Refresh Token 저장
        jwtTokenProvider.storeRefreshToken(newTokenId, user.id, familyId)

        return TokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds()
        )
    }

    /**
     * 로그아웃
     * 1. Access Token을 블랙리스트에 추가
     * 2. Redis에서 Refresh Token 삭제
     */
    fun logout(accessToken: String) {
        jwtTokenProvider.blacklistToken(accessToken)

        // Redis에서 Refresh Token 삭제 (userId 기반)
        try {
            val userId = jwtTokenProvider.getUserIdFromToken(accessToken)
            jwtTokenProvider.deleteRefreshTokenByUserId(userId)
        } catch (e: Exception) {
            log.debug("Refresh token cleanup skipped during logout: {}", e.message)
        }
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
     * GPS 동의 약관 조회
     */
    @Transactional(readOnly = true)
    fun getGpsConsentTerms(): GpsConsentTermsResponse {
        val terms = agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse()
            .orElseThrow { TermsNotFoundException() }

        return GpsConsentTermsResponse(
            agreementNumber = terms.name,
            contents = terms.contents
        )
    }

    /**
     * GPS 동의 상태 조회
     */
    @Transactional(readOnly = true)
    fun getGpsConsentStatus(userId: Long): GpsConsentStatusResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        return GpsConsentStatusResponse(
            requiresGpsConsent = user.requiresGpsConsent()
        )
    }

    /**
     * GPS 동의 기록 + 갱신된 access token 반환
     * 1. 사용자 조회
     * 2. 약관 조회 (agreementNumber 지정 시 해당 약관, 미지정 시 활성 약관)
     * 3. 동의 이력 INSERT (agreementhistory__c)
     * 4. User 플래그 업데이트 + 저장
     * 5. 새 Access Token 발급
     */
    @Transactional
    fun recordGpsConsent(userId: Long, request: GpsConsentRequest? = null): GpsConsentRecordResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        val agreementNumber = request?.agreementNumber?.takeIf { it.isNotBlank() }
        val terms = if (agreementNumber != null) {
            agreementWordRepository.findByNameAndIsDeletedFalse(agreementNumber)
                .orElseThrow { TermsNotFoundException() }
        } else {
            agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse()
                .orElseThrow { TermsNotFoundException() }
        }

        agreementHistoryRepository.save(
            AgreementHistory(
                employeeId = user.id,
                agreementFlag = true,
                agreementDate = LocalDate.now(),
                agreementWordId = terms.id.toLong()
            )
        )

        user.recordGpsConsent(terms.name)
        userRepository.save(user)

        val accessToken = jwtTokenProvider.createAccessToken(user.id, user.role, true)

        return GpsConsentRecordResponse(
            accessToken = accessToken,
            expiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds()
        )
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
