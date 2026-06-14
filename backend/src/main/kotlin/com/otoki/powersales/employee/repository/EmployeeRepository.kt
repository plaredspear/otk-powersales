package com.otoki.powersales.employee.repository

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
     * 지점 코드 목록으로 사원 조회 (관리자 대시보드 기본현황 — status 무관 전체)
     *
     * 재직/휴직/연령/판촉·OSC 집계를 한 번에 수행하기 위해 status 무관 전량 조회.
     */
    fun findByCostCenterCodeIn(costCenterCodes: List<String>): List<Employee>

    /**
     * 대시보드 기본현황 집계 전용 projection 조회 (지점 스코프).
     *
     * 기본현황은 jobCode / status / birthDate 3개 필드만 쓰므로 entity 전 컬럼 적재 대신
     * [DashboardEmployeeProjection] 으로 전송량을 축소한다. status 무관 전량.
     */
    fun findProjectedByCostCenterCodeIn(costCenterCodes: List<String>): List<DashboardEmployeeProjection>

    /**
     * 대시보드 기본현황 집계 전용 projection 조회 (전사 — 전사 권한 스코프).
     *
     * [findProjectedByCostCenterCodeIn] 의 전사판. WHERE 없는 findAll() 의 entity 전 컬럼 적재 대신
     * 3개 필드만 가져온다.
     */
    fun findProjectedBy(): List<DashboardEmployeeProjection>

    /**
     * 진열스케줄 템플릿용 사원 조회
     * 조건: costCenterCode 일치, role 일치, appLoginActive=true, status 일치
     */
    fun findByCostCenterCodeAndRoleAndAppLoginActiveTrueAndStatus(
        costCenterCode: String,
        role: String,
        status: String
    ): List<Employee>

    /**
     * 사원번호 목록으로 일괄 조회 (Excel 업로드 검증용)
     */
    fun findByEmployeeCodeIn(employeeCodes: List<String>): List<Employee>

    /**
     * 조직(costCenterCode) + 역할(role)로 사원 조회 (여사원 일정관리)
     */
    fun findByCostCenterCodeAndRole(costCenterCode: String, role: String): List<Employee>

    /**
     * 조직(costCenterCode) + 역할(role)이 제외 목록에 없는 사원 조회 (조장 팀원 목록).
     *
     * 레거시 employeeMapper.xml `empSearch` 의 여사원 식별 방식 보존:
     * `appauthority != '조장' AND != '지점장'` (역필터). role 이 NULL 인 사원은
     * SQL `NOT IN` 의미상 제외되며, 이는 레거시 `!=` 비교의 NULL 제외와 일치한다.
     */
    fun findByCostCenterCodeAndRoleNotIn(costCenterCode: String, roles: Collection<String>): List<Employee>

    /**
     * 조직 목록(costCenterCode IN) + 역할(role)로 사원 일괄 조회 (진열스케줄 업로드 - 조장 조회)
     */
    fun findByCostCenterCodeInAndRole(costCenterCodes: List<String>, role: String): List<Employee>

    /**
     * 조직 목록(costCenterCode IN) + 역할(role) + 앱 로그인 활성으로 사원 조회
     */
    fun findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(costCenterCodes: List<String>, role: String): List<Employee>

    /**
     * 역할(role) + 상태(status)로 사원 조회 (전문행사조 엑셀 템플릿용)
     */
    fun findByRoleAndStatus(role: String, status: String): List<Employee>

    fun findByCrmWorkStartDateIsNotNullAndCrmWorkStartDateLessThanEqual(date: LocalDate): List<Employee>

    /**
     * 다중 코스트센터 + 역할 + 앱 로그인 활성 + 상태로 사원 조회 (영업지원실 다중 지점 템플릿용)
     */
    fun findByCostCenterCodeInAndRoleAndAppLoginActiveTrueAndStatus(
        costCenterCodes: List<String>,
        role: String,
        status: String
    ): List<Employee>
}

/**
 * 대시보드 기본현황 집계 전용 사원 projection — jobCode / status / birthDate 만 노출.
 *
 * 판촉·OSC 인원 / 재직·휴직 / 연령대 집계에 필요한 최소 필드만 가져와 entity 전 컬럼 적재를 피한다.
 */
interface DashboardEmployeeProjection {
    val jobCode: String?
    val status: String?
    val birthDate: String?
}
