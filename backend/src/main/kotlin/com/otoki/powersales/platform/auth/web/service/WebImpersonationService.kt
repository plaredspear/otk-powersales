package com.otoki.powersales.platform.auth.web.service

import com.otoki.powersales.platform.auth.exception.ImpersonationAdminNotFoundException
import com.otoki.powersales.platform.auth.exception.ImpersonationAlreadyActiveException
import com.otoki.powersales.platform.auth.exception.ImpersonationNotActiveException
import com.otoki.powersales.platform.auth.exception.ImpersonationSelfNotAllowedException
import com.otoki.powersales.platform.auth.exception.ImpersonationTargetInactiveException
import com.otoki.powersales.platform.auth.exception.ImpersonationTargetNotFoundException
import com.otoki.powersales.platform.auth.web.WebJwtService
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.auth.web.dto.WebImpersonationMeta
import com.otoki.powersales.platform.auth.web.dto.WebImpersonationStartRequest
import com.otoki.powersales.platform.auth.web.dto.WebImpersonationStartResponse
import com.otoki.powersales.platform.auth.web.dto.WebImpersonationStopResponse
import com.otoki.powersales.platform.auth.web.entity.ImpersonationLog
import com.otoki.powersales.platform.auth.web.repository.ImpersonationLogRepository
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Web 관리자 대행 로그인 (Impersonation) 서비스 (Spec #851).
 *
 * SF 표준 "Login As" 기능을 신규 Web 관리자 대시보드에 구현. `SYSTEM:MANAGE_USERS` 보유 관리자가
 * 다른 Web 사용자 계정으로 대행 토큰을 발급받아 그 사용자처럼 화면을 사용한다.
 *
 * 권한 가드(`MANAGE_USERS`)는 controller 의 `@RequiresSfPermission` 표준 가드가 담당 (start). stop 은
 * 대행 토큰 자체가 자격이므로 권한 가드 미부착 — 본 서비스에서 `impersonatedBy != null` 만 확인.
 *
 * 대행 토큰 식별자 모델 (§2.0): subject/user_id = 대상 사용자, impersonated_by = 관리자.
 * 토큰 발급/principal·summary 산출은 [WebAuthenticationService.issueTokensFor] 재사용.
 */
@Service
@Transactional(readOnly = true)
class WebImpersonationService(
    private val userRepository: UserRepository,
    private val webAuthenticationService: WebAuthenticationService,
    private val webJwtService: WebJwtService,
    private val impersonationLogRepository: ImpersonationLogRepository,
) {

    /**
     * 대행 시작.
     *
     * 분기:
     * 1. 권한(`MANAGE_USERS`) 은 controller `@RequiresSfPermission` 가 사전 차단 (미보유 시 PERMISSION_DENIED).
     * 2. 이미 대행 중이면 `IMPERSONATION_ALREADY_ACTIVE` (중첩 금지)
     * 3. 자기 자신 대상이면 `IMPERSONATION_SELF_NOT_ALLOWED`
     * 4. 대상 User 없으면 `IMPERSONATION_TARGET_NOT_FOUND`, 비활성이면 `IMPERSONATION_TARGET_INACTIVE`
     * 5. 대상 기준 대행 토큰 발급 (impersonated_by = 관리자) + `impersonation_log` 적재
     */
    @Transactional
    fun start(adminPrincipal: WebUserPrincipal, request: WebImpersonationStartRequest): WebImpersonationStartResponse {
        if (adminPrincipal.impersonatedBy != null) {
            throw ImpersonationAlreadyActiveException()
        }

        val adminUserId = adminPrincipal.userId
        val targetUserId = request.targetUserId!!

        if (targetUserId == adminUserId) {
            throw ImpersonationSelfNotAllowedException()
        }

        val targetUser: User = userRepository.findById(targetUserId)
            .orElseThrow { ImpersonationTargetNotFoundException() }

        if (!targetUser.isActive) {
            throw ImpersonationTargetInactiveException()
        }

        val adminUser: User = userRepository.findById(adminUserId)
            .orElseThrow { ImpersonationAdminNotFoundException() }

        val startedAt = LocalDateTime.now()
        val accessExpiresAt = startedAt.plusSeconds(webJwtService.getAccessTokenExpirationSeconds().toLong())

        val issued = webAuthenticationService.issueTokensFor(targetUser, impersonatedBy = adminUserId)

        impersonationLogRepository.save(
            ImpersonationLog(
                adminUserId = adminUserId,
                targetUserId = targetUserId,
                reason = request.reason,
                startedAt = startedAt,
                accessExpiresAt = accessExpiresAt,
            )
        )

        return WebImpersonationStartResponse(
            accessToken = issued.accessToken,
            refreshToken = issued.refreshToken,
            expiresIn = issued.expiresIn,
            impersonation = WebImpersonationMeta(
                impersonatedByUserId = adminUserId,
                impersonatedByName = adminUser.name,
                targetUserId = targetUserId,
                targetName = targetUser.name,
                startedAt = startedAt,
            ),
            user = issued.summary,
        )
    }

    /**
     * 대행 종료 — 관리자 토큰으로 복귀.
     *
     * 분기:
     * 1. 대행 중이 아니면 `IMPERSONATION_NOT_ACTIVE`
     * 2. `impersonatedBy` 관리자 User 조회 (없으면 `IMPERSONATION_ADMIN_NOT_FOUND`)
     * 3. 관리자 정상 토큰 재발급 (impersonated_by 없음) + 활성 log row 종료 처리
     */
    @Transactional
    fun stop(currentPrincipal: WebUserPrincipal): WebImpersonationStopResponse {
        val adminUserId = currentPrincipal.impersonatedBy
            ?: throw ImpersonationNotActiveException()
        val targetUserId = currentPrincipal.userId

        val adminUser: User = userRepository.findById(adminUserId)
            .orElseThrow { ImpersonationAdminNotFoundException() }

        // 활성 대행 log row 종료 처리 (가장 최근 미종료 1건).
        impersonationLogRepository
            .findFirstByAdminUserIdAndTargetUserIdAndEndedAtIsNullOrderByStartedAtDesc(adminUserId, targetUserId)
            ?.markEnded(LocalDateTime.now())

        val issued = webAuthenticationService.issueTokensFor(adminUser, impersonatedBy = null)

        return WebImpersonationStopResponse(
            accessToken = issued.accessToken,
            refreshToken = issued.refreshToken,
            expiresIn = issued.expiresIn,
            user = issued.summary,
        )
    }
}
