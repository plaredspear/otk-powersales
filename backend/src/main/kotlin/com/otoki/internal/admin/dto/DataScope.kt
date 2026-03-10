package com.otoki.internal.admin.dto

/**
 * 관리자 데이터 조회 범위
 * @param branchCodes 조회 가능한 지점 코드 목록 (costCenterCode 기준)
 * @param isAllBranches true면 전체 조회 (branchCodes 무시)
 */
data class DataScope(
    val branchCodes: List<String>,
    val isAllBranches: Boolean
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

    fun validateAccess(costCenterCode: String?): Boolean {
        if (isAllBranches) return true
        if (costCenterCode == null) return false
        return costCenterCode in branchCodes
    }
}
