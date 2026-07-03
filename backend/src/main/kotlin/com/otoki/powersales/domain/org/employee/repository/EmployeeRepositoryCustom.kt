package com.otoki.powersales.domain.org.employee.repository

import com.otoki.powersales.domain.org.employee.entity.Employee
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface EmployeeRepositoryCustom {

    fun findWithEmployeeInfoByEmployeeCode(employeeCode: String): Employee?

    fun findWithEmployeeInfoById(id: Long): Employee?

    fun findWithEmployeeInfoByStatus(status: String): List<Employee>

    fun findWithEmployeeInfoByCostCenterCodeInAndStatus(costCenterCodes: List<String>, status: String): List<Employee>

    fun findWithEmployeeInfoByCostCenterCodeAndRole(costCenterCode: String, role: String): List<Employee>

    /**
     * SF 레거시 `TeamMemberListController.fetchTeamMembers()` 정합.
     *
     * `DKRetail__Employee__c WHERE CostCenterCode__c IN :codes AND DKRetail__AppAuthority__c='여사원'
     *                              AND DKRetail__APPLoginActive__c=true ORDER BY Name`
     *
     * @param costCenterCodes  필터링할 cost center 코드 집합. `null` 또는 비어있으면 전사 조회.
     */
    fun findActiveWomenByCostCenterCodes(costCenterCodes: List<String>?): List<Employee>

    /**
     * 여사원 목록 조회 — [findActiveWomenByCostCenterCodes] 와 달리 `app_login_active` 조건을 제외해
     * 퇴사/휴직 등 비활성 여사원도 포함한다. 근무기간 조회(과거 근무내역 조회) 화면 전용.
     *
     * `DKRetail__Employee__c WHERE CostCenterCode__c IN :codes AND DKRetail__AppAuthority__c='여사원'
     *                              AND is_deleted != true ORDER BY Name` (APPLoginActive 필터 없음)
     *
     * @param costCenterCodes  필터링할 cost center 코드 집합. `null` 또는 비어있으면 전사 조회.
     */
    fun findWomenByCostCenterCodes(costCenterCodes: List<String>?): List<Employee>

    fun findAllEmployeeCodes(): List<String>

    /**
     * @param role   단일 role 등호 필터 (`employee.role = :role`). 전체 사원 관리/lookup 화면용.
     * @param roles  다중 role IN 필터 (`employee.role IN :roles`). 여사원 현황(여사원+조장)처럼 여러
     *               직책을 함께 노출하는 화면용. [role] 과 [roles] 를 동시에 주면 둘 다 AND 로 적용된다
     *               (실사용은 둘 중 하나만 지정).
     */
    fun findEmployees(
        status: String?,
        branchCodes: List<String>?,
        keyword: String?,
        role: String?,
        roles: List<String>?,
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

    /**
     * SF `UplExcelBtnSchduleMasterController.checkResult` (L181) 정합 —
     * `Employee WHERE CostCenterCode__c IN :newOrgValues AND DKRetail__EmpCode__c IN :empCodes`.
     * BranchCodeExpander 확장 결과로 조장 지점 (이력 합집합) 필터 + 사번 필터 동시 적용.
     */
    fun findByCostCenterCodeInAndEmployeeCodeIn(
        costCenterCodes: Collection<String>,
        employeeCodes: Collection<String>
    ): List<Employee>
}
