package com.otoki.powersales.employee.repository

import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.employee.entity.Employee
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EmployeeRepositoryCustom {

    fun findWithEmployeeInfoByEmployeeCode(employeeCode: String): Employee?

    fun findWithEmployeeInfoById(id: Long): Employee?

    fun findWithEmployeeInfoByStatus(status: String): List<Employee>

    fun findWithEmployeeInfoByCostCenterCodeInAndStatus(costCenterCodes: List<String>, status: String): List<Employee>

    fun findWithEmployeeInfoByCostCenterCodeAndRole(costCenterCode: String, role: UserRoleEnum): List<Employee>

    /**
     * SF 레거시 `TeamMemberListController.fetchTeamMembers()` 정합.
     *
     * `DKRetail__Employee__c WHERE CostCenterCode__c IN :codes AND DKRetail__AppAuthority__c='여사원'
     *                              AND DKRetail__APPLoginActive__c=true ORDER BY Name`
     *
     * @param costCenterCodes  필터링할 cost center 코드 집합. `null` 또는 비어있으면 전사 조회.
     */
    fun findActiveWomenByCostCenterCodes(costCenterCodes: List<String>?): List<Employee>

    fun findAllEmployeeCodes(): List<String>

    fun findEmployees(
        status: String?,
        branchCodes: List<String>?,
        keyword: String?,
        role: UserRoleEnum?,
        pageable: Pageable
    ): Page<Employee>

    /**
     * 동의 플래그 활성 사원 일괄 false 갱신 (스펙 #654 / GPS 재동의 cycle batch — Q2 좁히기).
     *
     * 레거시 매핑: `AgreementWordBatch.cls:64-72` cascade reset.
     * 신규 차이: legacy 의 전 사원 SOQL → `WHERE agreement_flag=true` 좁히기. legacy `cls:70` 들여쓰기 버그 자연 회피.
     * 반환값: 영향받은 row 수 (운영 로그용).
     */
    fun resetAgreementFlagForActiveConsents(): Long
}
