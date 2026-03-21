package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.Employee
import com.otoki.internal.common.repository.EmployeeRepositoryCustom
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

/**
 * 사원 Repository
 */
interface EmployeeRepository : JpaRepository<Employee, Long>, EmployeeRepositoryCustom {

    /**
     * 사번으로 사원 조회
     */
    fun findByEmployeeNumber(employeeNumber: String): Optional<Employee>

    /**
     * 사번 존재 여부 확인
     */
    fun existsByEmployeeNumber(employeeNumber: String): Boolean

    /**
     * 조직별 사원 목록 조회
     */
    fun findByOrgName(orgName: String): List<Employee>

    /**
     * 지점 코드 목록 + 상태로 사원 조회 (관리자 대시보드 기본현황)
     */
    fun findByCostCenterCodeInAndStatus(costCenterCodes: List<String>, status: String): List<Employee>

    /**
     * 상태별 사원 전체 조회 (관리자 대시보드 - 전체 범위)
     */
    fun findByStatus(status: String): List<Employee>

    /**
     * sfid 목록으로 사원 조회
     */
    fun findBySfidIn(sfids: List<String>): List<Employee>

    /**
     * 진열스케줄 템플릿용 사원 조회
     * 조건: costCenterCode 일치, appAuthority IS NULL, appLoginActive=true, status 일치
     */
    fun findByCostCenterCodeAndAppAuthorityIsNullAndAppLoginActiveTrueAndStatus(
        costCenterCode: String,
        status: String
    ): List<Employee>

    /**
     * 사원번호 목록으로 일괄 조회 (Excel 업로드 검증용)
     */
    fun findByEmployeeNumberIn(employeeNumbers: List<String>): List<Employee>

    /**
     * 조직(costCenterCode) + 권한(appAuthority)으로 사원 조회 (여사원 일정관리)
     */
    fun findByCostCenterCodeAndAppAuthority(costCenterCode: String, appAuthority: String): List<Employee>

    /**
     * sfid로 사원 단건 조회 (여사원 일정관리 - 일정 등록 시 사원 검증)
     */
    fun findBySfid(sfid: String): Employee?

    /**
     * 조직 목록(costCenterCode IN) + 권한(appAuthority)으로 사원 일괄 조회 (진열스케줄 업로드 - 조장 조회)
     */
    fun findByCostCenterCodeInAndAppAuthority(costCenterCodes: List<String>, appAuthority: String): List<Employee>

    /**
     * 조직 목록(costCenterCode IN) + 권한(appAuthority) + 앱 로그인 활성(appLoginActive=true)으로 사원 조회
     * 진열스케줄 업로드 확정 시 활성 조장만 ownerId로 설정하기 위해 사용
     */
    fun findByCostCenterCodeInAndAppAuthorityAndAppLoginActiveTrue(costCenterCodes: List<String>, appAuthority: String): List<Employee>
}
