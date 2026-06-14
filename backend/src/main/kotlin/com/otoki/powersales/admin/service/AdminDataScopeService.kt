package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.platform.auth.repository.ProfileRepository
import com.otoki.powersales.platform.auth.sharing.repository.SharingPolicyQueryRepository
import com.otoki.powersales.platform.auth.sharing.service.GroupMembershipEvaluator
import com.otoki.powersales.platform.auth.sharing.service.PermissionSetEvaluator
import com.otoki.powersales.platform.auth.sharing.service.ProfileFlagsEvaluator
import com.otoki.powersales.platform.auth.sharing.service.RecordTypePermissionEvaluator
import com.otoki.powersales.platform.auth.sharing.service.UserRoleHierarchyTraversal
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminDataScopeService(
    private val employeeRepository: EmployeeRepository,
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository,
    private val userRoleHierarchyTraversal: UserRoleHierarchyTraversal,
    private val groupMembershipEvaluator: GroupMembershipEvaluator,
    private val profileFlagsEvaluator: ProfileFlagsEvaluator,
    private val permissionSetEvaluator: PermissionSetEvaluator,
    private val sharingPolicyQueryRepository: SharingPolicyQueryRepository,
    private val recordTypePermissionEvaluator: RecordTypePermissionEvaluator,
) {

    private val log = LoggerFactory.getLogger(AdminDataScopeService::class.java)

    /**
     * 기존 API (backward compat) — Employee.id 기준. sharing policy 신규 차원 미채움.
     */
    fun resolve(userId: Long): DataScope {
        val employee = employeeRepository.findWithEmployeeInfoById(userId)
            ?: throw IllegalStateException("사용자를 찾을 수 없습니다: $userId")
        val user = employee.employeeCode?.let { userRepository.findByEmployeeCode(it) }
        val profileName = user?.profileId?.let { profileRepository.findById(it).orElse(null)?.name }
        return resolveLegacy(profileName, user?.isSalesSupport ?: false, employee.costCenterCode)
    }

    /**
     * 기존 API (backward compat) — Employee 직접. sharing policy 신규 차원 미채움.
     */
    fun resolve(employee: Employee): DataScope {
        val user = employee.employeeCode?.let { userRepository.findByEmployeeCode(it) }
        val profileName = user?.profileId?.let { profileRepository.findById(it).orElse(null)?.name }
        return resolveLegacy(profileName, user?.isSalesSupport ?: false, employee.costCenterCode)
    }

    /**
     * 신규 admin read 진입점 — principal.userId 는 User.id (spec #782 P3-B).
     * 4 evaluator 호출 + sharingRule 본문 일람 조회로 DataScope 의 신규 차원 채움.
     */
    fun resolve(principal: WebUserPrincipal): DataScope {
        val legacyScope = resolveLegacy(principal.profileName, principal.isSalesSupport, principal.costCenterCode)
        return enrichWithSharingPolicy(legacyScope, principal.userId)
    }

    /**
     * 기존 4 분기 (spec #759~#780) — backward compat.
     * Profile.name == "시스템 관리자" OR isSalesSupport OR ALL_BRANCHES_PROFILES → isAllBranches=true
     * 그 외 → costCenterCode scope.
     *
     * SF 정합: AppointmentTriggerHanlder.cls:344-365 의 Profile.Name 분기 + Org.OrgCodeLevel3='3475' (영업지원실).
     */
    private fun resolveLegacy(profileName: String?, isSalesSupport: Boolean, costCenterCode: String?): DataScope {
        val isAllBranches = profileName == SYSTEM_ADMIN_PROFILE_NAME ||
            isSalesSupport ||
            profileName in ALL_BRANCHES_PROFILES
        return if (isAllBranches) {
            DataScope(branchCodes = emptyList(), isAllBranches = true)
        } else {
            DataScope(branchCodes = listOfNotNull(costCenterCode), isAllBranches = false)
        }
    }

    companion object {
        private const val SYSTEM_ADMIN_PROFILE_NAME = "시스템 관리자"
        private val ALL_BRANCHES_PROFILES: Set<String> = setOf(
            "1.본부장", "2.사업부장", "3.영업부장"
        )
    }

    /**
     * 신규 sharing policy 차원 populating (spec #782 P3-B).
     *
     * 본 User 의 4 evaluator 결과 + sharingRule 본문 일람 조회 후 DataScope 의 신규 필드 채움.
     * 기존 `branchCodes` / `isAllBranches` 는 그대로 유지 (backward compat).
     */
    private fun enrichWithSharingPolicy(legacyScope: DataScope, userId: Long): DataScope {
        val user = userRepository.findById(userId).orElse(null)
        if (user == null) {
            log.warn("[data-scope] User {} not found — sharing policy 차원 미채움", userId)
            return legacyScope
        }
        val userRoleId = user.userRoleId
        val ancestorPath = if (userRoleId != null) {
            try {
                userRoleHierarchyTraversal.getAncestorPath(userRoleId)
            } catch (e: IllegalStateException) {
                log.warn("[data-scope] hierarchy issue for userRoleId={} — fallback self only", userRoleId, e)
                listOf(userRoleId)
            }
        } else {
            emptyList()
        }
        val allSubordinates = if (userRoleId != null) {
            userRoleHierarchyTraversal.getAllSubordinateUserRoleIds(userRoleId)
        } else {
            emptySet()
        }
        val groupMemberships = groupMembershipEvaluator.getMemberGroupIds(userId, userRoleId)
        val profileFlags = profileFlagsEvaluator.getProfileFlags(userId)
        val permissionSetFlags = permissionSetEvaluator.getPermissionSetSnapshot(userId)
        val evaluatorRules = sharingPolicyQueryRepository.findRulesForUser(
            userId = userId,
            userRoleId = userRoleId,
            ancestorPath = ancestorPath,
            groupMemberships = groupMemberships,
        )

        // spec #796 — Record Type 가시성 채움
        val visibleRecordTypeIds = recordTypePermissionEvaluator.visibleRecordTypeIds(
            userId = userId,
            profileId = user.profileId,
            permissionSetIds = permissionSetFlags.permissionSetIds,
        )

        return legacyScope.copy(
            userId = userId,
            userRoleId = userRoleId,
            allSubordinateUserRoleIds = allSubordinates,
            profileFlags = profileFlags,
            groupMemberships = groupMemberships,
            permissionSetFlags = permissionSetFlags,
            evaluatorRules = evaluatorRules,
            visibleRecordTypeIds = visibleRecordTypeIds,
        )
    }
}
