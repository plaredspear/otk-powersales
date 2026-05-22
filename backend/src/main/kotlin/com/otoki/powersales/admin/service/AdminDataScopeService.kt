package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.auth.sharing.repository.SharingPolicyQueryRepository
import com.otoki.powersales.auth.sharing.service.GroupMembershipEvaluator
import com.otoki.powersales.auth.sharing.service.PermissionSetEvaluator
import com.otoki.powersales.auth.sharing.service.ProfileFlagsEvaluator
import com.otoki.powersales.auth.sharing.service.UserRoleHierarchyTraversal
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminDataScopeService(
    private val employeeRepository: EmployeeRepository,
    private val userRepository: UserRepository,
    private val userRoleHierarchyTraversal: UserRoleHierarchyTraversal,
    private val groupMembershipEvaluator: GroupMembershipEvaluator,
    private val profileFlagsEvaluator: ProfileFlagsEvaluator,
    private val permissionSetEvaluator: PermissionSetEvaluator,
    private val sharingPolicyQueryRepository: SharingPolicyQueryRepository,
) {

    private val log = LoggerFactory.getLogger(AdminDataScopeService::class.java)

    /**
     * 기존 API (backward compat) — Employee.id 기준. sharing policy 신규 차원 미채움.
     */
    fun resolve(userId: Long): DataScope {
        val employee = employeeRepository.findWithEmployeeInfoById(userId)
            ?: throw IllegalStateException("사용자를 찾을 수 없습니다: $userId")
        return resolveLegacy(employee.role, employee.costCenterCode)
    }

    /**
     * 기존 API (backward compat) — Employee 직접. sharing policy 신규 차원 미채움.
     */
    fun resolve(employee: Employee): DataScope = resolveLegacy(employee.role, employee.costCenterCode)

    /**
     * 신규 admin read 진입점 — principal.userId 는 User.id (spec #782 P3-B).
     * 4 evaluator 호출 + sharingRule 본문 일람 조회로 DataScope 의 신규 차원 채움.
     */
    fun resolve(principal: WebUserPrincipal): DataScope {
        val legacyScope = resolveLegacy(principal.role, principal.costCenterCode)
        return enrichWithSharingPolicy(legacyScope, principal.userId)
    }

    /**
     * 기존 4 분기 (spec #759~#780) — backward compat.
     * SYSTEM_ADMIN / ALL_BRANCHES → isAllBranches=true / UNKNOWN → 빈 scope / BRANCH_SCOPE (+ null) → costCenter scope.
     */
    private fun resolveLegacy(role: UserRoleEnum?, costCenterCode: String?): DataScope {
        return when {
            role == UserRoleEnum.SYSTEM_ADMIN -> DataScope(
                branchCodes = emptyList(),
                isAllBranches = true,
            )
            role in UserRoleEnum.ALL_BRANCHES -> DataScope(
                branchCodes = emptyList(),
                isAllBranches = true,
            )
            role == UserRoleEnum.UNKNOWN -> DataScope(
                branchCodes = emptyList(),
                isAllBranches = false,
            )
            role in UserRoleEnum.BRANCH_SCOPE || role == null -> DataScope(
                branchCodes = listOfNotNull(costCenterCode),
                isAllBranches = false,
            )
            else -> DataScope(
                branchCodes = listOfNotNull(costCenterCode),
                isAllBranches = false,
            )
        }
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

        return legacyScope.copy(
            userId = userId,
            userRoleId = userRoleId,
            allSubordinateUserRoleIds = allSubordinates,
            profileFlags = profileFlags,
            groupMemberships = groupMemberships,
            permissionSetFlags = permissionSetFlags,
            evaluatorRules = evaluatorRules,
        )
    }
}
