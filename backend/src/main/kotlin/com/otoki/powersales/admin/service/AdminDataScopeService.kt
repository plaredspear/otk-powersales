package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminDataScopeService(
    private val employeeRepository: EmployeeRepository
) {

    companion object {
        private val ALL_BRANCHES_AUTHORITIES = setOf("영업부장", "사업부장", "영업본부장", "영업지원실")
        private val BRANCH_ONLY_AUTHORITIES = setOf("조장", "지점장")
    }

    fun resolve(userId: Long): DataScope {
        val employee = employeeRepository.findWithEmployeeInfoById(userId)
            ?: throw IllegalStateException("사용자를 찾을 수 없습니다: $userId")
        return resolve(employee)
    }

    fun resolve(employee: Employee): DataScope {
        val authority = employee.appAuthority

        return when {
            authority in ALL_BRANCHES_AUTHORITIES -> DataScope(
                branchCodes = emptyList(),
                isAllBranches = true
            )
            authority in BRANCH_ONLY_AUTHORITIES || authority == null -> DataScope(
                branchCodes = listOfNotNull(employee.costCenterCode),
                isAllBranches = false
            )
            else -> DataScope(
                branchCodes = listOfNotNull(employee.costCenterCode),
                isAllBranches = false
            )
        }
    }
}
