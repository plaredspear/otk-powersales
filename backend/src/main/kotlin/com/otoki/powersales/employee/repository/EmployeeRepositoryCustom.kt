package com.otoki.powersales.employee.repository

import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.dto.response.BranchResponse
import com.otoki.powersales.employee.entity.Employee
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EmployeeRepositoryCustom {

    fun findWithEmployeeInfoByEmployeeCode(employeeCode: String): Employee?

    fun findWithEmployeeInfoById(id: Long): Employee?

    fun findWithEmployeeInfoByStatus(status: String): List<Employee>

    fun findWithEmployeeInfoByCostCenterCodeInAndStatus(costCenterCodes: List<String>, status: String): List<Employee>

    fun findWithEmployeeInfoByCostCenterCodeAndRole(costCenterCode: String, role: UserRole): List<Employee>

    fun findDistinctBranches(): List<BranchResponse>

    fun findAllEmployeeCodes(): List<String>

    fun findEmployees(
        status: String?,
        branchCodes: List<String>?,
        keyword: String?,
        role: UserRole?,
        pageable: Pageable
    ): Page<Employee>
}
