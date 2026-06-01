package com.otoki.powersales.auth.web.service

import com.otoki.powersales.auth.exception.ImpersonationAdminNotFoundException
import com.otoki.powersales.auth.exception.ImpersonationAlreadyActiveException
import com.otoki.powersales.auth.exception.ImpersonationNotActiveException
import com.otoki.powersales.auth.exception.ImpersonationSelfNotAllowedException
import com.otoki.powersales.auth.exception.ImpersonationTargetInactiveException
import com.otoki.powersales.auth.exception.ImpersonationTargetNotFoundException
import com.otoki.powersales.auth.web.WebJwtService
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.auth.web.dto.WebImpersonationStartRequest
import com.otoki.powersales.auth.web.dto.WebUserSummary
import com.otoki.powersales.auth.web.entity.ImpersonationLog
import com.otoki.powersales.auth.web.repository.ImpersonationLogRepository
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional

@DisplayName("WebImpersonationService 테스트")
class WebImpersonationServiceTest {

    private val userRepository: UserRepository = mockk()
    private val webAuthenticationService: WebAuthenticationService = mockk()
    private val webJwtService: WebJwtService = mockk()
    private val impersonationLogRepository: ImpersonationLogRepository = mockk(relaxUnitFun = true)

    private val service = WebImpersonationService(
        userRepository,
        webAuthenticationService,
        webJwtService,
        impersonationLogRepository,
    )

    @Nested
    @DisplayName("start - 대행 시작")
    inner class StartTests {

        @Test
        @DisplayName("성공 - 활성 대상 → 대행 토큰 발급 + impersonated_by = 관리자 + log 적재")
        fun start_success() {
            val admin = createUser(id = 7L, username = "admin@otokims.co.kr", name = "차영아")
            val target = createUser(id = 1234L, username = "hong@otokims.co.kr", name = "홍길동")
            val adminPrincipal = principalOf(admin)

            every { userRepository.findById(1234L) } returns Optional.of(target)
            every { userRepository.findById(7L) } returns Optional.of(admin)
            every { webJwtService.getAccessTokenExpirationSeconds() } returns 1800
            every { webAuthenticationService.issueTokensFor(target, 7L) } returns IssuedWebTokens(
                accessToken = "imp-access",
                refreshToken = "imp-refresh",
                expiresIn = 1800,
                summary = summaryOf(target),
            )
            val logSlot = slot<ImpersonationLog>()
            every { impersonationLogRepository.save(capture(logSlot)) } answers { logSlot.captured }

            val response = service.start(adminPrincipal, WebImpersonationStartRequest(1234L, "권한 재현"))

            assertThat(response.accessToken).isEqualTo("imp-access")
            assertThat(response.impersonation.impersonatedByUserId).isEqualTo(7L)
            assertThat(response.impersonation.impersonatedByName).isEqualTo("차영아")
            assertThat(response.impersonation.targetUserId).isEqualTo(1234L)
            assertThat(response.impersonation.targetName).isEqualTo("홍길동")
            assertThat(response.user.userId).isEqualTo(1234L)
            assertThat(logSlot.captured.adminUserId).isEqualTo(7L)
            assertThat(logSlot.captured.targetUserId).isEqualTo(1234L)
            assertThat(logSlot.captured.reason).isEqualTo("권한 재현")
            assertThat(logSlot.captured.endedAt).isNull()
            verify { webAuthenticationService.issueTokensFor(target, 7L) }
        }

        @Test
        @DisplayName("실패 - 이미 대행 중 → IMPERSONATION_ALREADY_ACTIVE")
        fun start_alreadyActive() {
            val admin = createUser(id = 7L)
            val adminPrincipal = principalOf(admin).copy(impersonatedBy = 99L)

            assertThatThrownBy { service.start(adminPrincipal, WebImpersonationStartRequest(1234L, null)) }
                .isInstanceOf(ImpersonationAlreadyActiveException::class.java)
        }

        @Test
        @DisplayName("실패 - 자기 자신 대상 → IMPERSONATION_SELF_NOT_ALLOWED")
        fun start_self() {
            val admin = createUser(id = 7L)
            val adminPrincipal = principalOf(admin)

            assertThatThrownBy { service.start(adminPrincipal, WebImpersonationStartRequest(7L, null)) }
                .isInstanceOf(ImpersonationSelfNotAllowedException::class.java)
        }

        @Test
        @DisplayName("실패 - 대상 User 없음 → IMPERSONATION_TARGET_NOT_FOUND")
        fun start_targetNotFound() {
            val admin = createUser(id = 7L)
            val adminPrincipal = principalOf(admin)
            every { userRepository.findById(1234L) } returns Optional.empty()

            assertThatThrownBy { service.start(adminPrincipal, WebImpersonationStartRequest(1234L, null)) }
                .isInstanceOf(ImpersonationTargetNotFoundException::class.java)
        }

        @Test
        @DisplayName("실패 - 대상 비활성 → IMPERSONATION_TARGET_INACTIVE")
        fun start_targetInactive() {
            val admin = createUser(id = 7L)
            val target = createUser(id = 1234L, isActive = false)
            val adminPrincipal = principalOf(admin)
            every { userRepository.findById(1234L) } returns Optional.of(target)

            assertThatThrownBy { service.start(adminPrincipal, WebImpersonationStartRequest(1234L, null)) }
                .isInstanceOf(ImpersonationTargetInactiveException::class.java)
        }
    }

    @Nested
    @DisplayName("stop - 대행 종료")
    inner class StopTests {

        @Test
        @DisplayName("성공 - 대행 토큰 → 관리자 토큰 재발급 + 활성 log row 종료")
        fun stop_success() {
            val admin = createUser(id = 7L, username = "admin@otokims.co.kr", name = "차영아")
            // 대행 중 principal: user_id = 대상(1234), impersonatedBy = 관리자(7)
            val target = createUser(id = 1234L)
            val currentPrincipal = principalOf(target).copy(impersonatedBy = 7L)
            val activeLog = ImpersonationLog(
                adminUserId = 7L,
                targetUserId = 1234L,
                startedAt = LocalDateTime.now().minusMinutes(5),
                accessExpiresAt = LocalDateTime.now().plusMinutes(25),
            )

            every { userRepository.findById(7L) } returns Optional.of(admin)
            every {
                impersonationLogRepository.findFirstByAdminUserIdAndTargetUserIdAndEndedAtIsNullOrderByStartedAtDesc(7L, 1234L)
            } returns activeLog
            every { webAuthenticationService.issueTokensFor(admin, null) } returns IssuedWebTokens(
                accessToken = "admin-access",
                refreshToken = "admin-refresh",
                expiresIn = 1800,
                summary = summaryOf(admin),
            )

            val response = service.stop(currentPrincipal)

            assertThat(response.accessToken).isEqualTo("admin-access")
            assertThat(response.user.userId).isEqualTo(7L)
            assertThat(activeLog.endedAt).isNotNull()
            verify { webAuthenticationService.issueTokensFor(admin, null) }
        }

        @Test
        @DisplayName("성공 - 활성 log row 없어도 토큰 재발급은 정상 (만료 후 stop)")
        fun stop_noActiveLog() {
            val admin = createUser(id = 7L)
            val target = createUser(id = 1234L)
            val currentPrincipal = principalOf(target).copy(impersonatedBy = 7L)

            every { userRepository.findById(7L) } returns Optional.of(admin)
            every {
                impersonationLogRepository.findFirstByAdminUserIdAndTargetUserIdAndEndedAtIsNullOrderByStartedAtDesc(7L, 1234L)
            } returns null
            every { webAuthenticationService.issueTokensFor(admin, null) } returns IssuedWebTokens(
                "a", "r", 1800, summaryOf(admin)
            )

            val response = service.stop(currentPrincipal)

            assertThat(response.accessToken).isEqualTo("a")
        }

        @Test
        @DisplayName("실패 - 대행 중 아님 (impersonatedBy=null) → IMPERSONATION_NOT_ACTIVE")
        fun stop_notActive() {
            val user = createUser(id = 1L)
            val principal = principalOf(user)

            assertThatThrownBy { service.stop(principal) }
                .isInstanceOf(ImpersonationNotActiveException::class.java)
        }

        @Test
        @DisplayName("실패 - 복귀 관리자 User 없음 → IMPERSONATION_ADMIN_NOT_FOUND")
        fun stop_adminNotFound() {
            val target = createUser(id = 1234L)
            val currentPrincipal = principalOf(target).copy(impersonatedBy = 7L)
            every { userRepository.findById(7L) } returns Optional.empty()

            assertThatThrownBy { service.stop(currentPrincipal) }
                .isInstanceOf(ImpersonationAdminNotFoundException::class.java)
        }
    }

    private fun createUser(
        id: Long = 1L,
        username: String = "u@otokims.co.kr",
        name: String = "사용자",
        isActive: Boolean = true,
    ): User = User(
        id = id,
        username = username,
        isActive = isActive,
        employeeCode = null,
        isSalesSupport = false,
        password = "\$2a\$10\$hash",
        passwordChangeRequired = false
    ).also { it.name = name }

    private fun principalOf(user: User): WebUserPrincipal = WebUserPrincipal(
        userId = user.id,
        usernameValue = user.username,
        employeeCode = user.employeeCode,
        employeeId = null,
        role = null,
        costCenterCode = null,
        isSalesSupport = false,
        passwordChangeRequired = false,
        permissions = emptySet(),
        encodedPassword = user.password,
        grantedAuthorities = emptyList(),
        active = user.isActive
    )

    private fun summaryOf(user: User): WebUserSummary = WebUserSummary(
        userId = user.id,
        username = user.username,
        name = user.name,
        employeeCode = user.employeeCode,
        profileName = null,
        isSalesSupport = false,
        role = null,
        orgName = null,
        costCenterCode = null,
        permissions = emptyList()
    )
}
