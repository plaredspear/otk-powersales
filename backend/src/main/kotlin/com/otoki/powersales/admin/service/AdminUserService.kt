package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.AdminUserDetailResponse
import com.otoki.powersales.admin.dto.AdminUserListItem
import com.otoki.powersales.admin.dto.AdminUserListResponse
import com.otoki.powersales.admin.dto.AdminUserPasswordResetResponse
import com.otoki.powersales.admin.dto.AdminUserProfileOption
import com.otoki.powersales.admin.exception.AdminProfileNotFoundException
import com.otoki.powersales.admin.exception.AdminUserNotFoundException
import com.otoki.powersales.admin.exception.CannotChangeOwnProfileException
import com.otoki.powersales.admin.exception.CannotDeactivateSelfException
import com.otoki.powersales.admin.exception.ProfileChangeForbiddenException
import com.otoki.powersales.admin.security.AdminDataScopeCache
import com.otoki.powersales.platform.auth.permission.AdminPermissionCache
import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.platform.auth.policy.TemporaryPasswordPolicy
import com.otoki.powersales.platform.auth.repository.ProfileRepository
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * web admin User 관리 화면 서비스.
 *
 * - 목록/상세 조회 (USER_READ)
 * - 비밀번호 임시 리셋 → "{사번}@pwrs" ([TemporaryPasswordPolicy]) BCrypt 해시 + passwordChangeRequired = true.
 *   사원과 매칭되지 않아 `employeeCode` 가 없는 순수 관리자 계정은 종전 고정값으로 되돌아간다.
 * - 활성/비활성 토글 — 자기 자신 비활성화 시도는 차단
 *
 * 권한 게이트는 컨트롤러 단의 `@RequiresPermission` 가 처리하며, 본 서비스는 진입 시점에
 * 권한 보유를 가정한다.
 */
@Service
@Transactional(readOnly = true)
class AdminUserService(
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository,
    private val organizationRepository: OrganizationRepository,
    private val passwordEncoder: PasswordEncoder,
    private val adminPermissionCache: AdminPermissionCache,
    private val adminDataScopeCache: AdminDataScopeCache
) {

    private val logger = LoggerFactory.getLogger(AdminUserService::class.java)

    fun findUsers(
        keyword: String?,
        isActive: Boolean?,
        profileId: Long?,
        // 지점 미필터가 기본 — 사용자 lookup 으로 본 메소드를 빌려쓰는 호출처(현장점검 테마 등) 는 미전달.
        costCenterCode: String? = null,
        page: Int,
        size: Int
    ): AdminUserListResponse {
        val pageable = PageRequest.of(page, size)
        // 사용자 관리는 시스템 메뉴라 전사 조회 — costCenterCode 는 보안축이 아닌 순수 표시 필터다
        // (사원 목록의 `applyBranchScope = false` 경로와 동일 취급).
        val branchFilter = costCenterCode?.takeIf { it.isNotBlank() }?.let { listOf(it) }
        val userPage = userRepository.findUsers(keyword, isActive, profileId, branchFilter, pageable)

        // 페이지 내 distinct profileId 만 한 번에 lookup (N+1 회피).
        val profileNames = resolveProfileNames(userPage.content)

        return AdminUserListResponse(
            content = userPage.content.map { AdminUserListItem.from(it, profileNames[it.profileId]) },
            page = page,
            size = size,
            totalElements = userPage.totalElements,
            totalPages = userPage.totalPages
        )
    }

    /**
     * 사용자 관리 화면 필터용 프로파일 옵션 목록 (id/name, 이름 오름차순).
     *
     * 프로파일 관리 상세 목록(`profile` READ) 이 아니라 `user` READ 로 가드된 경량 lookup 이라,
     * 사용자 관리 권한만 가진 관리자도 필터 드롭다운을 채울 수 있다.
     */
    fun getProfileOptions(): List<AdminUserProfileOption> =
        profileRepository.findAll()
            .map { AdminUserProfileOption(id = it.id, name = it.name) }
            .sortedBy { it.name }

    /**
     * 사용자 관리 화면 필터용 지점 옵션 목록 (전사).
     *
     * 사원 목록의 `/admin/employees/branches` 와 동일한 전사 지점 목록이지만, 그쪽은 `employee` READ
     * 가드라 `user` 권한만 가진 관리자는 403 이 난다. 프로파일 옵션([getProfileOptions]) 과 같은 이유로
     * 화면 게이팅 권한(`user`) 과 동일하게 가드한 lookup 으로 분리한다.
     *
     * 사용자 관리는 시스템 메뉴이므로 목록 자체가 전사 조회 — 옵션도 전사로 내린다 (셀렉터-조회 스코프 동일).
     */
    fun getBranchOptions(): List<BranchResponse> =
        organizationRepository.findAllTeamScheduleBranches()

    fun findUserDetail(userId: Long): AdminUserDetailResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { AdminUserNotFoundException(userId) }
        val profileName = user.profileId?.let { profileRepository.findById(it).orElse(null)?.name }
        return AdminUserDetailResponse.from(user, profileName)
    }

    /** 사용자 목록의 profileId 집합을 Profile.name 으로 일괄 변환. */
    private fun resolveProfileNames(users: List<User>): Map<Long, String> {
        val profileIds = users.mapNotNull { it.profileId }.toSet()
        if (profileIds.isEmpty()) return emptyMap()
        return profileRepository.findAllById(profileIds).associate { it.id to it.name }
    }

    @Transactional
    fun resetPassword(userId: Long): AdminUserPasswordResetResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { AdminUserNotFoundException(userId) }

        val encoded = passwordEncoder.encode(TemporaryPasswordPolicy.forEmployeeCode(user.employeeCode))!!
        user.password = encoded
        user.passwordChangeRequired = true

        logger.info(
            "ADMIN_USER_PASSWORD_RESET target={} username={}",
            user.id,
            user.username
        )

        return AdminUserPasswordResetResponse.from(user)
    }

    @Transactional
    fun updateActiveStatus(targetUserId: Long, requesterUserId: Long, isActive: Boolean) {
        if (!isActive && targetUserId == requesterUserId) {
            throw CannotDeactivateSelfException()
        }

        val user = userRepository.findById(targetUserId)
            .orElseThrow { AdminUserNotFoundException(targetUserId) }

        if (user.isActive == isActive) return

        user.isActive = isActive

        logger.info(
            "ADMIN_USER_ACTIVE_STATUS_CHANGED target={} username={} newStatus={}",
            user.id,
            user.username,
            isActive
        )
    }

    /**
     * User 프로파일 수동 변경 — 시스템 관리자 전용.
     *
     * ## 왜 필요한가
     * 평시 `User.profileId` 는 SAP 발령 후처리([AppointmentUserProfileUpdater.updateUserProfileCache])
     * 가 사원의 조직(costCenterCode) + 직책(jikchak) 으로부터 [EmployeeProfileResolver] 로 산출한다.
     * 발령이 실제로 인입되기 전에는 권한을 줄 수단이 없어, 발령 예정일 이전 인수인계처럼 미리
     * 권한이 필요한 상황을 처리할 수 없었다. 본 경로가 그 산출값을 수동으로 덮어쓴다.
     *
     * ## 발령과의 관계 — 발령이 이깁니다 (사용자 결정)
     * 수동 변경은 **다음 발령 인입 전까지만** 유효하다. 해당 사원의 발령이 들어오면 후처리가
     * `profileId` 를 재산출해 덮어쓴다. dirty 플래그로 수동값을 보존하지 않는 이유는 SAP 가 인사의
     * SoT 이기 때문이며, 보존할 경우 조직개편 후에도 사원이 옛 프로파일에 고착될 수 있다.
     * 따라서 본 기능은 상시 권한 운영이 아니라 **한시적 예외 처리** 용도다.
     *
     * ## 가드
     * - 시스템 관리자만 호출 가능 — 프로파일은 권한 모델의 최상위 입력이라 `user:EDIT` 보유자에게
     *   열면 권한 자가 상승이 가능해진다.
     * - 자기 자신은 대상 불가 — 상승(승인 없는 권한 확대) / 하강(스스로 권한을 잃어 복구 불가) 양방향 차단.
     *
     * 변경 즉시 권한/데이터 스코프 캐시를 무효화한다. 누락 시 대상자는 재로그인 전까지 이전 권한으로
     * 동작해 "바꿨는데 안 바뀐다" 로 관측된다.
     *
     * @param targetUserId 변경 대상 User
     * @param requesterUserId 요청자 User (자기 자신 가드용)
     * @param requesterProfileName 요청자 Profile.name (시스템 관리자 판정용)
     * @param profileId 새 Profile
     */
    @Transactional
    fun updateProfile(
        targetUserId: Long,
        requesterUserId: Long,
        requesterProfileName: String?,
        profileId: Long
    ) {
        if (!SystemAdminProfilePolicy.isSystemAdmin(requesterProfileName)) {
            throw ProfileChangeForbiddenException()
        }
        if (targetUserId == requesterUserId) {
            throw CannotChangeOwnProfileException()
        }

        val user = userRepository.findById(targetUserId)
            .orElseThrow { AdminUserNotFoundException(targetUserId) }
        val profile = profileRepository.findById(profileId)
            .orElseThrow { AdminProfileNotFoundException(profileId) }

        val previousProfileId = user.profileId
        if (previousProfileId == profileId) return

        user.profileId = profileId

        // profileId 는 권한 산출의 입력이라 변경 즉시 파생 캐시를 버린다
        // (AppointmentUserProfileUpdater.updateUserProfileCache 와 동일 처리).
        adminPermissionCache.invalidate(user.id)
        adminDataScopeCache.invalidate(user.id)

        logger.info(
            "ADMIN_USER_PROFILE_CHANGED target={} username={} previousProfileId={} newProfileId={} newProfileName={} requester={}",
            user.id,
            user.username,
            previousProfileId,
            profileId,
            profile.name,
            requesterUserId
        )
    }
}
