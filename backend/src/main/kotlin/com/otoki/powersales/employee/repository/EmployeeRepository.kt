package com.otoki.powersales.employee.repository

import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.employee.entity.Employee
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.Optional

/**
 * 사원 Repository
 */
interface EmployeeRepository : JpaRepository<Employee, Long>, EmployeeRepositoryCustom {

    /**
     * 사번으로 사원 조회
     */
    fun findByEmployeeCode(employeeCode: String): Optional<Employee>

    /**
     * 사번 존재 여부 확인
     */
    fun existsByEmployeeCode(employeeCode: String): Boolean

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
     * 진열스케줄 템플릿용 사원 조회
     * 조건: costCenterCode 일치, role 일치, appLoginActive=true, status 일치
     */
    fun findByCostCenterCodeAndRoleAndAppLoginActiveTrueAndStatus(
        costCenterCode: String,
        role: UserRole,
        status: String
    ): List<Employee>

    /**
     * 사원번호 목록으로 일괄 조회 (Excel 업로드 검증용)
     */
    fun findByEmployeeCodeIn(employeeCodes: List<String>): List<Employee>

    /**
     * 조직(costCenterCode) + 역할(role)로 사원 조회 (여사원 일정관리)
     */
    fun findByCostCenterCodeAndRole(costCenterCode: String, role: UserRole): List<Employee>

    /**
     * 조직 목록(costCenterCode IN) + 역할(role)로 사원 일괄 조회 (진열스케줄 업로드 - 조장 조회)
     */
    fun findByCostCenterCodeInAndRole(costCenterCodes: List<String>, role: UserRole): List<Employee>

    /**
     * 조직 목록(costCenterCode IN) + 역할(role) + 앱 로그인 활성으로 사원 조회
     */
    fun findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(costCenterCodes: List<String>, role: UserRole): List<Employee>

    /**
     * 역할(role) + 상태(status)로 사원 조회 (전문행사조 엑셀 템플릿용)
     */
    fun findByRoleAndStatus(role: UserRole, status: String): List<Employee>

    fun findByCrmWorkStartDateIsNotNullAndCrmWorkStartDateLessThanEqual(date: LocalDate): List<Employee>

    /**
     * 다중 코스트센터 + 역할 + 앱 로그인 활성 + 상태로 사원 조회 (영업지원실 다중 지점 템플릿용)
     */
    fun findByCostCenterCodeInAndRoleAndAppLoginActiveTrueAndStatus(
        costCenterCodes: List<String>,
        role: UserRole,
        status: String
    ): List<Employee>
}
