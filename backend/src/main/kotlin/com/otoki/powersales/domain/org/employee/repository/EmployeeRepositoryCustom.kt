package com.otoki.powersales.domain.org.employee.repository

import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
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
     * 행사사원 후보 여사원 검색 전용 — SF 레거시 `RelatedListDataGridController.getLookupCandidates` 정합.
     *
     * SF 원본 lookup 은 `DKRetail__Status__c='재직'` 만 걸고 `APPLoginActive` 는 걸지 않는다
     * (여사원일정 계열 [findActiveWomenByCostCenterCodes] 와 다른 축). 확정 검증
     * ([com.otoki.powersales.domain.activity.promotion.service.PromotionSchedulesUpsertHelper] 의 status
     * '휴직'/'퇴직' 차단) 과 동일하게 `status='재직'` 으로 걸어, lookup↔확정 기준을 status 로 일치시켜
     * "퇴직인데 후보엔 뜨는" 불일치를 제거한다.
     *
     * `DKRetail__Employee__c WHERE CostCenterCode__c IN :codes AND DKRetail__AppAuthority__c='여사원'
     *                              AND DKRetail__Status__c='재직' AND is_deleted != true ORDER BY Name`
     *
     * @param costCenterCodes  필터링할 cost center 코드 집합. `null` 또는 비어있으면 전사 조회.
     */
    fun findActiveWomenForPromotionByCostCenterCodes(costCenterCodes: List<String>?): List<Employee>

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
     * 관리자 대시보드 기본현황 집계 전용 projection 조회 — 여사원(role='여사원')만.
     *
     * 기본현황은 jobCode / status / birthDate 3개 필드만 쓰므로 entity 전 컬럼 적재 대신
     * [DashboardEmployeeProjection] 으로 전송량을 축소한다.
     *
     * 조장/지점장/관리직은 제외하고 여사원만 집계한다.
     * 퇴직자(status='퇴직') 는 재직 현황 모수에서 제외한다. status 가 NULL 인 사원은
     * 재직/휴직 미분류로 유지해야 하므로 포함한다.
     *
     * @param costCenterCodes  지점 스코프 필터. `null` 또는 비어있으면 전사 조회.
     */
    fun findDashboardBasicStatsProjection(costCenterCodes: List<String>?): List<DashboardEmployeeProjection>

    /**
     * @param role   단일 role 등호 필터 (`employee.role = :role`). 전체 사원 관리/lookup 화면용.
     * @param roles  다중 role IN 필터 (`employee.role IN :roles`). 여사원 현황(여사원+조장)처럼 여러
     *               직책을 함께 노출하는 화면용. [role] 과 [roles] 를 동시에 주면 둘 다 AND 로 적용된다
     *               (실사용은 둘 중 하나만 지정).
     * @param workTypeMatchedEmployeeIds  근무형태(1/3) 필터 결과 employee_id 집합. 근무형태 필터를 건 경우
     *               서비스 레이어가 [com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepositoryCustom.findEmployeeIdsMatchingLatestWorkType]
     *               로 '최근 출근등록 1건이 조건과 일치하는 사원' 집합을 먼저 산출해 넘긴다. `employee.id IN (...)`
     *               로 필터하며(상관 서브쿼리 제거 — timeout 회피), **null = 근무형태 필터 미적용**,
     *               **비어있는 집합 = 일치 사원 0명(빈 결과)**. 두 의미를 구분한다.
     * @param promotionTeam  전문행사조 필터 — 지정 시 `employee.professionalPromotionTeam = :promotionTeam`.
     *                       null 이면 미적용 (단 [promotionTeamGeneral] 참조).
     * @param promotionTeamGeneral  '일반'(전문행사조 미배정 = null) 필터 여부. true 면
     *                              `employee.professionalPromotionTeam IS NULL`. [promotionTeam] 과 상호배타.
     */
    fun findEmployees(
        status: String?,
        branchCodes: List<String>?,
        keyword: String?,
        role: String?,
        roles: List<String>?,
        workTypeMatchedEmployeeIds: Set<Long>? = null,
        promotionTeam: ProfessionalPromotionTeamType? = null,
        promotionTeamGeneral: Boolean = false,
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

    /**
     * 외부 조회(OVIP inbound) 전량 스냅샷 — PK keyset 페이지 1건 분량.
     *
     * MFEIS 스냅샷과 달리 **entity 전 컬럼**을 노출하므로 projection 축소 없이 entity 를 그대로 반환한다.
     * `employeeInfo` 는 fetch join 하지 않는다 — 비밀번호 해시/기기 식별자를 담은 인증 정보라 외부 노출
     * 대상이 아니며, 접근하지 않으므로 추가 SELECT 도 발생하지 않는다.
     *
     * 관계 FK(ownerUser/ownerGroup/createdBy/lastModifiedBy/manager/postponedAppointment)는
     * **entity 가 아니라 [EmployeeSnapshotRow] 의 별도 필드로 함께 select** 한다. `entity.manager?.id` 로
     * 읽어도 (식별자 접근은 프록시를 초기화하지 않으므로) 추가 쿼리는 없지만, FK 를 쿼리에서 직접
     * 가져오면 관계 필드에 의존하지 않아 그 성질이 깨질 여지 자체가 없다. 회귀 감시는
     * `OvipSnapshotKeysetRepositoryTest` 의 **쿼리 1회** 단언이 담당한다.
     *
     * @param cursor 직전 페이지 마지막 id. null 이면 처음부터
     * @param limit  조회 상한 (hasNext 판정을 위해 호출 측이 pageSize + 1 을 넘긴다)
     */
    fun findSnapshotByKeyset(cursor: Long?, limit: Int): List<EmployeeSnapshotRow>
}

/**
 * 사원 전량 스냅샷 1건 — entity + 관계 FK id.
 *
 * 관계 FK 를 entity 의 LAZY 필드에 의존하지 않고 쿼리에서 함께 뽑아 오기 위한 묶음
 * (근거는 [EmployeeRepositoryCustom.findSnapshotByKeyset] KDoc).
 */
data class EmployeeSnapshotRow(
    val employee: Employee,
    val ownerUserId: Long?,
    val ownerGroupId: Long?,
    val createdById: Long?,
    val lastModifiedById: Long?,
    val managerId: Long?,
    val postponedAppointmentId: Long?,
)
