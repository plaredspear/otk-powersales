package com.otoki.powersales.domain.org.employee.repository

import com.otoki.powersales.domain.org.employee.entity.Employee
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
     * 코스트센터(costCenterCode) 단위 사원 목록 조회.
     *
     * 레거시 홈(home.jsp) 조장 팀 범위 정합 — 레거시는 조장과 동일 costcentercode 전원을
     * 팀원으로 집계한다(employeeMapper `costcentercode__c = (…teamleadercode…)` 서브쿼리).
     */
    fun findByCostCenterCode(costCenterCode: String): List<Employee>

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
     * SF Id(sfid) 목록으로 일괄 조회 — SF fetch sync 의 사원 FK resolve 용.
     */
    fun findBySfidIn(sfids: Collection<String>): List<Employee>

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

    /**
     * 발령일이 당일 도래한 예약 발령 사원 조회 (연기예약 처리 배치).
     *
     * SF `PostponedAppointmentBatch.cls:15` 정합 —
     * `CRM_WorkStartDate__c = :today AND PostponedAppointment__c != null` 을 그대로 재현한다.
     *
     * - **등호 조건**: `<=` 로 완화하면 과거 일자 잔여 예약(SF 가 영영 반영하지 않는 죽은 상태 —
     *   마이그레이션 재유입 포함)까지 반영해 SF 와 어긋난다. SF 동일하게 등호를 쓰며, 배치가 당일
     *   실행되지 못하면 그 날짜 예약이 미반영으로 남는 것 또한 SF 와 동일한 알려진 동작이다
     *   (복구는 발령 재수신 또는 수동 정정).
     * - **참조 non-null**: 참조 없는 예약 잔여는 SF 처럼 조회 단계에서 배제해 건드리지 않는다.
     */
    fun findByCrmWorkStartDateAndPostponedAppointmentIsNotNull(date: LocalDate): List<Employee>

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

    /** 직책명 — 직급별 인원현황의 '판매조장' 그룹 판정 축 (`jikchak = '판매조장'`). */
    val jikchak: String?

    /** 직위명 — 직급별 인원현황의 2단(직급) 축. OSPM/OSPE/OSPJ/OSC 외 값도 존재하는 자유 텍스트. */
    val jikwee: String?
}

/**
 * [DashboardEmployeeProjection] 의 QueryDSL `Projections.constructor` 전용 구현체.
 *
 * QueryDSL 은 interface projection 을 직접 생성하지 못하므로, 조회 결과를 담을 concrete class 를 둔다.
 */
data class DashboardEmployeeProjectionDto(
    override val jobCode: String?,
    override val status: String?,
    override val birthDate: String?,
    override val jikchak: String?,
    override val jikwee: String?,
) : DashboardEmployeeProjection
