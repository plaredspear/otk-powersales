package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminDataScopeService(
    private val employeeRepository: EmployeeRepository
) {

    fun resolve(userId: Long): DataScope {
        val employee = employeeRepository.findWithEmployeeInfoById(userId)
            ?: throw IllegalStateException("사용자를 찾을 수 없습니다: $userId")
        return resolve(employee)
    }

    fun resolve(employee: Employee): DataScope {
        val role = employee.role

        return when {
            role == UserRole.SYSTEM_ADMIN -> DataScope(
                branchCodes = emptyList(),
                isAllBranches = true
            )
            role in UserRole.ALL_BRANCHES -> DataScope(
                branchCodes = emptyList(),
                isAllBranches = true
            )
            role == UserRole.UNKNOWN -> DataScope(
                branchCodes = emptyList(),
                isAllBranches = false
            )
            role in UserRole.BRANCH_SCOPE || role == null -> DataScope(
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
