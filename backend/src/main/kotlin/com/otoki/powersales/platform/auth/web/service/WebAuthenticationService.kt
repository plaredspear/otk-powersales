package com.otoki.powersales.platform.auth.web.service

import com.otoki.powersales.platform.auth.permission.SfPermissionResolver
import com.otoki.powersales.platform.auth.exception.CurrentPasswordRequiredException
import com.otoki.powersales.platform.auth.exception.ImpersonationPasswordChangeBlockedException
import com.otoki.powersales.platform.auth.exception.InvalidCredentialsException
import com.otoki.powersales.platform.auth.exception.InvalidCurrentPasswordException
import com.otoki.powersales.platform.auth.exception.InvalidTokenException
import com.otoki.powersales.platform.auth.exception.TokenReuseDetectedException
import com.otoki.powersales.platform.auth.exception.UserInactiveException
import com.otoki.powersales.platform.auth.policy.PasswordPolicyValidator
import com.otoki.powersales.platform.auth.web.WebJwtService
import com.otoki.powersales.platform.auth.web.WebRefreshTokenStore
import com.otoki.powersales.platform.auth.web.WebUserDetailsService
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.auth.web.dto.WebChangePasswordRequest
import com.otoki.powersales.platform.auth.web.dto.WebChangePasswordResponse
import com.otoki.powersales.platform.auth.web.dto.WebLoginRequest
import com.otoki.powersales.platform.auth.web.dto.WebLoginResponse
import com.otoki.powersales.platform.auth.web.dto.WebRefreshTokenRequest
import com.otoki.powersales.platform.auth.web.dto.WebTokenResponse
import com.otoki.powersales.platform.auth.web.dto.WebUserSummary
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.auth.repository.ProfileRepository
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

/**
 * Web 로그인 / 토큰 갱신 / 비밀번호 변경 서비스 (Spec #760).
 *
 * ## 레거시 매핑
 * - SF Apex: SF 플랫폼 표준 로그인 (SF org login) — backend 외부
 * - origin spec: #760
 *
 * ## 레거시 동작 요약
 * 1. 입력: SF Username (`DKRetail__Email__c`) + 비밀번호 (`System.resetPassword(...)` 으로 발급된 SF 비밀번호)
 * 2. SF 플랫폼이 자체 인증 처리 — backend 비참여
 * 3. 인증 성공 시 SF Session 발급
 * 4. 비밀번호 변경: SF 자체 화면 (`/secur/setpassword`)
 *
 * ## 신규 차이
 * - 인증 entity: SF User → backend `user` 테이블 + BCrypt. 참조: legacy-deviation.md §"인증·세션"
 * - 세션 모델: SF Session → JWT Access + Refresh (Rotation). 참조: legacy-deviation.md §"인증·세션"
 * - 비밀번호 초기화: SF `System.resetPassword` → 임시 비밀번호 + `password_change_required` 강제 변경 화면 (Q5 옵션 1).
 * - JWT audience claim 분리: `"web"` (본 service) / `"mobile"` ([com.otoki.powersales.platform.auth.service.AuthService]) — cross-platform 토큰 사용 차단.
 */
@Service
@Transactional(readOnly = true)
class WebAuthenticationService(
    private val userRepository: UserRepository,
    private val webUserDetailsService: WebUserDetailsService,
    private val webJwtService: WebJwtService,
    private val webRefreshTokenStore: WebRefreshTokenStore,
    private val passwordEncoder: PasswordEncoder,
    private val passwordPolicyValidator: PasswordPolicyValidator,
    private val employeeRepository: EmployeeRepository,
    private val sfPermissionResolver: SfPermissionResolver,
    private val profileRepository: ProfileRepository,
) {

    /**
     * Web 로그인.
     *
     * 분기:
     * 1. Username 으로 User 조회 (없으면 `INVALID_CREDENTIALS`)
     * 2. `is_active == false` 면 `USER_INACTIVE` (계정 비활성)
     * 3. BCrypt 비밀번호 비교 (불일치 시 `INVALID_CREDENTIALS`)
     * 4. Access + Refresh Token 발급 (audience="web") + Redis 저장
     * 5. `User.last_login_at` 갱신 (audit)
     */
    @Transactional
    fun login(request: WebLoginRequest): WebLoginResponse {
        val user = userRepository.findByUsername(request.username)
            ?: throw InvalidCredentialsException()

        if (!user.isActive) {
            throw UserInactiveException()
        }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw InvalidCredentialsException()
        }

        user.recordLogin(LocalDateTime.now())

        // employeeCode 부재 user (SF 시스템 user / 부서 공용 계정) 는 Employee 매칭 자체 skip.
        val employee: Employee? = user.employeeCode?.let { employeeRepository.findByEmployeeCode(it).orElse(null) }
        val permissions: Set<String> = sfPermissionResolver.resolveForUser(user)
        val principal = principalFor(user, employee, permissions)
        val summary = summaryFor(user, employee, permissions)
        val familyId = UUID.randomUUID().toString()
        val tokenId = UUID.randomUUID().toString()

        val accessToken = webJwtService.createAccessToken(principal, summary.role)
        val refreshToken = webJwtService.createRefreshToken(
            username = user.username,
            userId = user.id,
            familyId = familyId,
            tokenId = tokenId
        )
        webRefreshTokenStore.store(tokenId, user.id, familyId, webJwtService.getRefreshExpirationMillis())

        return WebLoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = webJwtService.getAccessTokenExpirationSeconds(),
            passwordChangeRequired = user.passwordChangeRequired ?: true,
            user = summary
        )
    }

    /**
     * Refresh Token Rotation.
     *
     * 분기:
     * 1. Refresh Token 검증 (audience="web" + type="refresh" + 서명 + 만료)
     * 2. Family 무효화 여부 — revoked 면 `TOKEN_REUSE_DETECTED`
     * 3. Redis 메타 존재 여부 — 부재면 재사용 감지 → family revoke + `TOKEN_REUSE_DETECTED`
     * 4. 이전 token 삭제 → 새 tokenId 발급 → 새 access + refresh token 발급 + Redis 저장
     */
    @Transactional
    fun refresh(request: WebRefreshTokenRequest): WebTokenResponse {
        val token = request.refreshToken
        if (!webJwtService.validateRefreshToken(token)) {
            throw InvalidTokenException()
        }

        val familyId = webJwtService.getFamilyIdFromToken(token)
        val tokenId = webJwtService.getTokenIdFromToken(token)
        val userId = webJwtService.getUserIdFromToken(token)

        if (webRefreshTokenStore.isFamilyRevoked(familyId)) {
            throw TokenReuseDetectedException()
        }

        if (!webRefreshTokenStore.exists(tokenId)) {
            webRefreshTokenStore.revokeFamily(familyId, webJwtService.getRefreshExpirationMillis())
            throw TokenReuseDetectedException()
        }

        // 대행 토큰이면 impersonated_by claim 을 새 토큰에 보존 (§851 Q1) — 대행 세션을 refresh 만료까지 유지.
        // user_id claim 이 대상 사용자 PK 이므로 아래 findById 가 그대로 대상 권한을 재산출 (§851 §2.0/§2.4).
        val impersonatedBy: Long? = webJwtService.getImpersonatedByFromToken(token)

        webRefreshTokenStore.delete(tokenId)

        val user = userRepository.findById(userId).orElseThrow { InvalidTokenException() }
        val employee: Employee? = user.employeeCode?.let { employeeRepository.findByEmployeeCode(it).orElse(null) }
        val permissions: Set<String> = sfPermissionResolver.resolveForUser(user)
        val principal = principalFor(user, employee, permissions)
        val summary = summaryFor(user, employee, permissions)

        val newTokenId = UUID.randomUUID().toString()
        val newAccessToken = webJwtService.createAccessToken(principal, summary.role, impersonatedBy)
        val newRefreshToken = webJwtService.createRefreshToken(
            username = user.username,
            userId = user.id,
            familyId = familyId,
            tokenId = newTokenId,
            impersonatedBy = impersonatedBy
        )
        webRefreshTokenStore.store(newTokenId, user.id, familyId, webJwtService.getRefreshExpirationMillis())

        return WebTokenResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = webJwtService.getAccessTokenExpirationSeconds()
        )
    }

    /**
     * Web 비밀번호 변경 (강제 / 자발 통합).
     *
     * 분기 (principal.passwordChangeRequired 기반):
     * - true (강제): `currentPassword` 미검증
     * - false (자발): `currentPassword` 누락 시 `AUTH_CURRENT_PASSWORD_REQUIRED`, 불일치 시 `AUTH_CURRENT_PASSWORD_MISMATCH`
     *
     * 처리: 새 비밀번호 정책 검증 → BCrypt 해시 → `User.changePassword(...)`.
     * 토큰 재발급은 본 spec 범위 외 (frontend 가 다음 호출 전 재로그인 또는 별도 endpoint 사용).
     */
    @Transactional
    fun changePassword(
        principal: WebUserPrincipal,
        request: WebChangePasswordRequest
    ): WebChangePasswordResponse {
        // 대행 중에는 대상 사용자 비밀번호를 변경할 수 없다 (§851 §2.5).
        if (principal.impersonatedBy != null) {
            throw ImpersonationPasswordChangeBlockedException()
        }

        val user = userRepository.findById(principal.userId).orElseThrow { InvalidCredentialsException() }

        if (!principal.passwordChangeRequired) {
            val currentPassword = request.currentPassword?.takeIf { it.isNotBlank() }
                ?: throw CurrentPasswordRequiredException()
            if (!passwordEncoder.matches(currentPassword, user.password)) {
                throw InvalidCurrentPasswordException()
            }
        }

        passwordPolicyValidator.validate(request.newPassword)

        val encoded = passwordEncoder.encode(request.newPassword)!!
        user.changePassword(encoded)

        return WebChangePasswordResponse(passwordChangeRequired = false)
    }

    /**
     * `WebUserSummary` 산출 — Web Admin UI 의 라우트 가드 / 권한 가드 / 데이터 스코프 분기에 필요한
     * Employee + 권한 정보를 1회 응답에 함께 담는다.
     *
     * Employee 미존재 (예: ADMIN-* 부트스트랩 직후 또는 SAP 미동기 상태) 시 role/orgName/permissions 는
     * null/빈 배열로 fallback. 동일 정보가 JWT claim 으로도 발급된다.
     */
    private fun summaryFor(user: User, employee: Employee?, permissions: Set<String>): WebUserSummary {
        val profileName: String? = user.profileId?.let { profileRepository.findById(it).orElse(null)?.name }
        return WebUserSummary(
            userId = user.id,
            username = user.username,
            name = user.name,
            employeeCode = user.employeeCode,
            profileName = profileName,
            isSalesSupport = user.isSalesSupport ?: false,
            role = employee?.role,
            orgName = employee?.orgName,
            costCenterCode = user.costCenterCode,
            permissions = permissions.toList()
        )
    }

    /**
     * 지정 User 기준으로 새 토큰 쌍 + summary 를 발급하고 Redis 에 refresh 메타를 저장한다 (Spec #851 재사용).
     *
     * `impersonatedBy != null` 이면 대행 토큰 — access/refresh 모두 `impersonated_by` claim 을 담는다.
     * subject/user_id 는 항상 인자 `user` 기준이므로, 대행 시 대상 사용자를 넘기면 권한 평가가 대상 기준이 된다 (§851 §2.0).
     */
    @Transactional
    fun issueTokensFor(user: User, impersonatedBy: Long? = null): IssuedWebTokens {
        val employee: Employee? = user.employeeCode?.let { employeeRepository.findByEmployeeCode(it).orElse(null) }
        val permissions: Set<String> = sfPermissionResolver.resolveForUser(user)
        val principal = principalFor(user, employee, permissions)
        val summary = summaryFor(user, employee, permissions)
        val familyId = UUID.randomUUID().toString()
        val tokenId = UUID.randomUUID().toString()

        val accessToken = webJwtService.createAccessToken(principal, summary.role, impersonatedBy)
        val refreshToken = webJwtService.createRefreshToken(
            username = user.username,
            userId = user.id,
            familyId = familyId,
            tokenId = tokenId,
            impersonatedBy = impersonatedBy
        )
        webRefreshTokenStore.store(tokenId, user.id, familyId, webJwtService.getRefreshExpirationMillis())

        return IssuedWebTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = webJwtService.getAccessTokenExpirationSeconds(),
            summary = summary
        )
    }

    private fun principalFor(user: User, employee: Employee?, permissions: Set<String>): WebUserPrincipal {
        val profileName: String? = user.profileId?.let { profileRepository.findById(it).orElse(null)?.name }
        return WebUserPrincipal(
            userId = user.id,
            usernameValue = user.username,
            employeeCode = user.employeeCode,
            employeeId = employee?.id,
            role = employee?.role,
            costCenterCode = user.costCenterCode,
            profileName = profileName,
            profileId = user.profileId,
            isSalesSupport = user.isSalesSupport ?: false,
            passwordChangeRequired = user.passwordChangeRequired ?: true,
            permissions = permissions,
            encodedPassword = user.password,
            grantedAuthorities = WebUserDetailsService.resolveAuthoritiesByProfileName(
                profileName,
                user.isSalesSupport ?: false
            ),
            active = user.isActive
        )
    }
}

/**
 * 토큰 발급 결과 홀더 ([WebAuthenticationService.issueTokensFor] 반환, Spec #851).
 */
data class IssuedWebTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val summary: WebUserSummary,
)
