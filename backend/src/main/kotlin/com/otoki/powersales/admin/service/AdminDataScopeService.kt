package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.auth.web.WebUserPrincipal
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
        return resolve(employee.role, employee.costCenterCode)
    }

    fun resolve(employee: Employee): DataScope = resolve(employee.role, employee.costCenterCode)

    fun resolve(principal: WebUserPrincipal): DataScope = resolve(principal.role, principal.costCenterCode)

    private fun resolve(role: UserRole?, costCenterCode: String?): DataScope {
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
                branchCodes = listOfNotNull(costCenterCode),
                isAllBranches = false
            )
            else -> DataScope(
                branchCodes = listOfNotNull(costCenterCode),
                isAllBranches = false
            )
        }
    }
}
