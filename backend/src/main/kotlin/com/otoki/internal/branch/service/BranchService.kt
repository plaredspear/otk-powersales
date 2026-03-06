package com.otoki.internal.branch.service

import com.otoki.internal.branch.dto.response.BranchResponse
import com.otoki.internal.sap.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BranchService(
    private val userRepository: UserRepository
) {

    fun getBranches(): List<BranchResponse> {
        return userRepository.findDistinctBranches()
            .filter { it.branchName.isNotBlank() }
            .sortedBy { it.branchName }
    }
}
