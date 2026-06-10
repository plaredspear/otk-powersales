package com.otoki.powersales.auth.service

import com.otoki.powersales.common.config.UuidCheckProperties
import com.otoki.powersales.auth.dto.request.ChangePasswordRequest
import com.otoki.powersales.common.dto.request.GpsConsentRequest
import com.otoki.powersales.auth.dto.request.LoginRequest
import com.otoki.powersales.auth.dto.request.RefreshTokenRequest
import com.otoki.powersales.auth.dto.request.VerifyPasswordRequest
import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.auth.dto.response.*
import com.otoki.powersales.auth.policy.PasswordPolicyValidator
import com.otoki.powersales.common.dto.response.*
import com.otoki.powersales.common.entity.AgreementHistory
import com.otoki.powersales.common.entity.LoginHistory
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.auth.exception.*
import com.otoki.powersales.common.repository.AgreementHistoryRepository
import com.otoki.powersales.common.repository.AgreementWordRepository
import com.otoki.powersales.common.repository.LoginHistoryRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.common.util.TimeZones
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
@Transactional(readOnly = true)
class AuthService(
    private val employeeRepository: EmployeeRepository,
    private val loginHistoryRepository: LoginHistoryRepository,
    private val agreementWordRepository: AgreementWordRepository,
    private val agreementHistoryRepository: AgreementHistoryRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val uuidCheckProperties: UuidCheckProperties,
    private val passwordPolicyValidator: PasswordPolicyValidator
) {

    private val log = LoggerFactory.getLogger(AuthService::class.java)

    /**
     * 로그인
     * 1. 사번으로 사용자 조회 (없으면 INVALID_CREDENTIALS)
     * 2. BCrypt 비밀번호 검증 (불일치 시 INVALID_CREDENTIALS)
     * 3. 단말기 바인딩 검증 (device_id 존재 시)
     * 4. Access Token + Refresh Token 생성 (familyId, tokenId 포함)
     * 5. Redis에 Refresh Token 메타데이터 저장
     * 6. passwordChangeRequired, requiresGpsConsent 포함하여 반환
     */
    @Transactional
    fun login(request: LoginRequest): LoginResponse {
        val employee = employeeRepository.findWithEmployeeInfoByEmployeeCode(request.employeeCode)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(request.password, employee.password)) {
            throw InvalidCredentialsException()
        }

        // 로그인 권한 검증 (비밀번호 검증 이후, 단말기 바인딩 이전)
        validateLoginAuthority(employee)

        // 단말기 바인딩 검증 (비밀번호 검증 이후)
        validateDeviceBinding(employee, request.deviceId)

        // 로그인 이력 기록 (실패해도 로그인에 영향 없음)
        try {
            loginHistoryRepository.save(
                LoginHistory(empCode = employee.employeeCode ?: error("로그인 사원의 사번이 null - 비정상"))
            )
        } catch (e: Exception) {
            log.warn("로그인 이력 기록 실패: employeeCode={}", employee.employeeCode, e)
        }

        val passwordChangeRequired = employee.passwordChangeRequired ?: true
        val accessToken = jwtTokenProvider.createAccessToken(
            employee.id,
            employee.role ?: AppAuthority.WOMAN,
            employee.agreementFlag == true,
            passwordChangeRequired
        )

        // Refresh Token Rotation: 신규 familyId 로 발급
        val refreshToken = issueRefreshToken(employee.id, UUID.randomUUID().toString())

        return LoginResponse(
            user = UserInfo.from(employee),
            token = TokenInfo(
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds()
            ),
            passwordChangeRequired = passwordChangeRequired,
            requiresGpsConsent = employee.requiresGpsConsent()
        )
    }

    /**
     * 단말기 바인딩 검증
     * 1. 바인딩 비활성화 → 검증 스킵
     * 2. 예외 사번 → 검증 스킵
     * 3. DB deviceUuid == NULL → 최초 등록
     * 4. 일치 → 정상
     * 5. 불일치 → DEVICE_MISMATCH 에러
     */
    private fun validateDeviceBinding(employee: Employee, deviceId: String) {
        if (!uuidCheckProperties.enabled) return
        if (uuidCheckProperties.isExcluded(employee.employeeCode ?: error("로그인 사원의 사번이 null - 비정상"))) return

        if (employee.deviceUuid == null) {
            employee.bindDevice(deviceId)
            employeeRepository.save(employee)
            return
        }

        if (employee.deviceUuid != deviceId) {
            throw DeviceMismatchException()
        }
    }

    /**
     * 로그인 권한 검증 (native app 전용 — appLoginActive 가 true 여야 함).
     *
     * 신규 Web(admin) 로그인은 `/api/v1/admin/auth/login` + WebAuthenticationService 경로
     * (User 기반) 로 완전히 분리되어 있어, 본 모바일 엔드포인트는 Mobile 권한만 검증한다.
     */
    private fun validateLoginAuthority(employee: Employee) {
        if (employee.appLoginActive != true) {
            throw AppLoginNotActiveException()
        }
    }

    /**
     * 비밀번호 변경 (강제/자발 통합 — Spec #584).
     *
     * 분기 (토큰 클레임 `passwordChangeRequired` 기반):
     * - true (강제 변경): `currentPassword` 무시 (전달되어도 미검증).
     * - false (자발 변경): `currentPassword` 누락 시 `AUTH_CURRENT_PASSWORD_REQUIRED`,
     *                       BCrypt 불일치 시 `AUTH_CURRENT_PASSWORD_MISMATCH`.
     *
     * 처리:
     * 1. 새 비밀번호 정책 검증 ([PasswordPolicyValidator])
     * 2. BCrypt 암호화 + `Employee.changePassword(...)` (passwordChangeRequired=false 자동)
     * 3. 새 토큰 페어 발급 (familyId 신규, 클레임 `passwordChangeRequired=false` 반영)
     * 4. Redis 에 새 refresh token 저장
     */
    @Transactional
    fun changePassword(principal: UserPrincipal, request: ChangePasswordRequest): ChangePasswordResponse {
        val employee = employeeRepository.findWithEmployeeInfoById(principal.userId)
            ?: throw EmployeeNotFoundException()

        if (!principal.passwordChangeRequired) {
            val currentPassword = request.currentPassword
                ?.takeIf { it.isNotBlank() }
                ?: throw CurrentPasswordRequiredException()
            if (!passwordEncoder.matches(currentPassword, employee.password)) {
                throw InvalidCurrentPasswordException()
            }
        }

        passwordPolicyValidator.validate(request.newPassword)

        val encodedPassword = passwordEncoder.encode(request.newPassword)!!
        employee.changePassword(encodedPassword)
        employeeRepository.save(employee)

        val accessToken = jwtTokenProvider.createAccessToken(
            employee.id,
            employee.role ?: AppAuthority.WOMAN,
            employee.agreementFlag == true,
            false
        )
        val refreshToken = issueRefreshToken(employee.id, UUID.randomUUID().toString())

        return ChangePasswordResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds()
        )
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
        val employee = employeeRepository.findById(userId)
            .orElseThrow { InvalidTokenException() }

        // 7. 새 토큰 발급 (동일 familyId 재사용, 새 tokenId)
        val newAccessToken = jwtTokenProvider.createAccessToken(employee.id, employee.role ?: AppAuthority.WOMAN, employee.agreementFlag == true)
        val newRefreshToken = issueRefreshToken(employee.id, familyId)

        return TokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds()
        )
    }

    /**
     * Refresh Token 발급 + Redis 메타데이터 저장.
     *
     * 신규 발급(login/changePassword)은 호출처에서 새 [familyId] 를 생성해 전달하고,
     * rotation(refreshAccessToken)은 기존 [familyId] 를 그대로 전달해 family 를 유지한다.
     */
    private fun issueRefreshToken(employeeId: Long, familyId: String): String {
        val tokenId = UUID.randomUUID().toString()
        val refreshToken = jwtTokenProvider.createRefreshToken(employeeId, familyId, tokenId)
        jwtTokenProvider.storeRefreshToken(tokenId, employeeId, familyId)
        return refreshToken
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
    fun verifyPassword(userId: Long, request: VerifyPasswordRequest) {
        val employee = employeeRepository.findWithEmployeeInfoById(userId)
            ?: throw EmployeeNotFoundException()

        if (!passwordEncoder.matches(request.currentPassword, employee.password)) {
            throw InvalidCurrentPasswordException()
        }
    }

    /**
     * GPS 동의 약관 조회
     */
    fun getGpsConsentTerms(): GpsConsentTermsResponse {
        val terms = agreementWordRepository.findFirstByActiveTrueAndIsDeletedFalse()
            .orElseThrow { TermsNotFoundException() }

        return GpsConsentTermsResponse(
            agreementNumber = terms.name,
            contents = terms.contents
        )
    }

    /**
     * 현재 사용자 정보 조회 (자동로그인 후 사용자 복원용 — Spec #B-me)
     *
     * Access Token 의 subject(userId) 로 사원을 조회하여 로그인 응답과 동일한
     * [UserInfo] 를 반환한다. refresh 응답은 토큰만 주므로, 모바일이 자동로그인
     * 직후 이 엔드포인트로 사용자 정보를 확보해 인증 완료 상태로 전환한다.
     */
    fun getMe(userId: Long): UserInfo {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }
        return UserInfo.from(employee)
    }

    /**
     * GPS 동의 상태 조회
     */
    fun getGpsConsentStatus(userId: Long): GpsConsentStatusResponse {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        return GpsConsentStatusResponse(
            requiresGpsConsent = employee.requiresGpsConsent()
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
        val employee = employeeRepository.findWithEmployeeInfoById(userId)
            ?: throw EmployeeNotFoundException()

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
                employeeId = employee.id,
                agreementFlag = true,
                agreementDate = LocalDate.now(TimeZones.SEOUL_ZONE),
                agreementWordId = terms.id.toLong()
            )
        )

        employee.recordGpsConsent(terms.name)

        val accessToken = jwtTokenProvider.createAccessToken(employee.id, employee.role ?: AppAuthority.WOMAN, true)

        return GpsConsentRecordResponse(
            accessToken = accessToken,
            expiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds()
        )
    }

    /**
     * 단말기 초기화 (관리자 전용)
     */
    @Transactional
    fun resetDevice(employeeCode: String) {
        val employee = employeeRepository.findWithEmployeeInfoByEmployeeCode(employeeCode)
            ?: throw EmployeeNotFoundException()

        employee.resetDevice()
        employeeRepository.save(employee)
    }

}
