package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.DataScope
import com.otoki.internal.sap.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminDataScopeService(
    private val userRepository: UserRepository
) {

    companion object {
        private val ALL_BRANCHES_AUTHORITIES = setOf("영업부장", "사업부장", "영업본부장", "영업지원실")
        private val BRANCH_ONLY_AUTHORITIES = setOf("조장", "지점장")
    }

    fun resolve(userId: Long): DataScope {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalStateException("사용자를 찾을 수 없습니다: $userId") }

        val authority = user.appAuthority

        return when {
            authority in ALL_BRANCHES_AUTHORITIES -> DataScope(
                branchCodes = emptyList(),
                isAllBranches = true
            )
            authority in BRANCH_ONLY_AUTHORITIES || authority == null -> DataScope(
                branchCodes = listOfNotNull(user.costCenterCode),
                isAllBranches = false
            )
            else -> DataScope(
                branchCodes = listOfNotNull(user.costCenterCode),
                isAllBranches = false
            )
        }
    }
}
