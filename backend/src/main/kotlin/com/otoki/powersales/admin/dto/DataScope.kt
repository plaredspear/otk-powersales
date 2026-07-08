package com.otoki.powersales.admin.dto

import com.otoki.powersales.platform.auth.sharing.dto.PermissionSetSnapshot
import com.otoki.powersales.platform.auth.sharing.dto.ProfileFlagsSnapshot
import com.otoki.powersales.platform.auth.sharing.dto.SharingRuleSnapshot

/**
 * 관리자 데이터 조회 범위.
 *
 * ## 기존 (spec #759 ~ #780)
 * `branchCodes` / `isAllBranches` 의 2차원 추상화로 SF Hierarchy + sharingRule 의 약 80% 흡수.
 *
 * ## 신규 (spec #782 P3-B)
 * SF Sharing Rule 정책 evaluator (SharingRulePolicyEvaluator) 가 매 admin read 시 본 DataScope 를
 * 입력으로 받아 QueryDSL Predicate 합성. 누락 위험 5건 (HR 부서코드 / CVS / Edit-Read 이중 / CreatedById /
 * BranchCode vs AccountBranchCode) 을 정밀 분기.
 *
 * 우선순위 평가 (높음 → 낮음):
 * 1. profileFlags.viewAllData / permissionSetFlags.viewAllData/viewAllRecords[SObject] — no-filter
 * 2. Owner 매칭 (record.owner_id = userId)
 * 3. UserRole Hierarchy (record.owner.user_role_id IN allSubordinateUserRoleIds)
 * 4. SharingRule 본문 (evaluatorRules 의 condition 평가)
 * 5. Legacy branchCodes (기존 추상화 그대로)
 * 6. ControlledByParent — 부모 SObject 의 가시성 흡수
 *
 * 신규 필드는 모두 default value (`null` / `emptySet()` / `NONE`) — 기존 호출 site backward compat.
 *
 * @param branchCodes 조회 가능한 지점 코드 목록 (costCenterCode 기준) — backward compat
 * @param isAllBranches true면 전체 조회 (branchCodes 무시) — backward compat
 * @param userId 본 평가 대상 User PK (신규)
 * @param userRoleId User.userRoleId (신규)
 * @param allSubordinateUserRoleIds UserRoleHierarchyTraversal 결과 (신규)
 * @param profileFlags Profile system 권한 비트 (신규)
 * @param groupMemberships GroupMembershipEvaluator 결과 — Group.id set (신규)
 * @param permissionSetFlags PermissionSet 권한 평가 결과 (신규)
 * @param accessLevel Read / Edit (조장 Edit / 영업사원 Read 이중 레이어 — 신규)
 * @param hrCodes HQReview 의 HR 부서코드 차원 (신규)
 * @param accountGroups Account 의 CVS 사업부 분기 (1000/1010/3000 — 신규)
 * @param ownerExceptions DisplayWorkScheduleMaster / MFEIS 의 CreatedById 예외 user_id 일람 (신규)
 * @param evaluatorRules 본 User 에게 매칭되는 sharingRule 본문 일람 (신규)
 * @param visibleRecordTypeIds Profile + PermissionSet 합산 가시 RT id 집합 (spec #794 — Q2 옵션 1: 빈 set 이면 record_type_id IS NOT NULL row 모두 차단)
 */
data class DataScope(
    val branchCodes: List<String>,
    val isAllBranches: Boolean,
    val userId: Long? = null,
    val userRoleId: Long? = null,
    val allSubordinateUserRoleIds: Set<Long> = emptySet(),
    val profileFlags: ProfileFlagsSnapshot = ProfileFlagsSnapshot.NONE,
    val groupMemberships: Set<Long> = emptySet(),
    val permissionSetFlags: PermissionSetSnapshot = PermissionSetSnapshot.NONE,
    val accessLevel: String = "Read",
    val hrCodes: List<String> = emptyList(),
    val accountGroups: List<String> = emptyList(),
    val ownerExceptions: List<Long> = emptyList(),
    val evaluatorRules: List<SharingRuleSnapshot> = emptyList(),
    val visibleRecordTypeIds: Set<Long> = emptySet(),
) {

    fun effectiveBranchCodes(requestedBranchCode: String?): EffectiveBranchResult {
        return when {
            isAllBranches && requestedBranchCode != null ->
                EffectiveBranchResult.Filtered(listOf(requestedBranchCode))
            isAllBranches ->
                EffectiveBranchResult.All
            !isAllBranches && requestedBranchCode != null ->
                if (requestedBranchCode in branchCodes) EffectiveBranchResult.Filtered(listOf(requestedBranchCode))
                else EffectiveBranchResult.NoAccess
            else ->
                if (branchCodes.isEmpty()) EffectiveBranchResult.NoAccess
                else EffectiveBranchResult.Filtered(branchCodes)
        }
    }

    /**
     * 다중 지점 선택 버전 — 화면이 여러 지점을 동시에 요청할 때 사용(예: 대시보드 다중 선택).
     * 단일 [effectiveBranchCodes] 와 동일한 IDOR 규칙을 코드 목록 전체에 적용한다.
     *
     * - `isAllBranches`: 선택 목록이 있으면 그대로 Filtered, 없으면 All.
     * - 지점 사용자: 선택 목록 ∩ 본인 branchCodes → 교집합이 비면 NoAccess(권한 밖 지점만 골랐거나 선택 없음).
     */
    fun effectiveBranchCodes(requestedBranchCodes: List<String>): EffectiveBranchResult {
        val requested = requestedBranchCodes.filter { it.isNotBlank() }.distinct()
        return when {
            isAllBranches && requested.isNotEmpty() -> EffectiveBranchResult.Filtered(requested)
            isAllBranches -> EffectiveBranchResult.All
            requested.isNotEmpty() -> {
                val allowed = requested.filter { it in branchCodes }
                if (allowed.isEmpty()) EffectiveBranchResult.NoAccess
                else EffectiveBranchResult.Filtered(allowed)
            }
            else ->
                if (branchCodes.isEmpty()) EffectiveBranchResult.NoAccess
                else EffectiveBranchResult.Filtered(branchCodes)
        }
    }

    fun validateAccess(costCenterCode: String?): Boolean {
        if (isAllBranches) return true
        if (costCenterCode == null) return false
        return costCenterCode in branchCodes
    }
}
