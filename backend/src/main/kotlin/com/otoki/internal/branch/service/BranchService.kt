package com.otoki.internal.branch.service

import com.otoki.internal.branch.dto.response.BranchResponse
import com.otoki.internal.sap.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BranchService(
    private val employeeRepository: EmployeeRepository
) {

    fun getBranches(): List<BranchResponse> {
        return employeeRepository.findDistinctBranches()
            .filter { it.branchName.isNotBlank() }
            .sortedBy { it.branchName }
    }
}
