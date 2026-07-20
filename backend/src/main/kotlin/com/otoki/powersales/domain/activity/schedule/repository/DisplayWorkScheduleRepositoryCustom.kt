package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.activity.schedule.enums.SchedulePreset
import com.otoki.powersales.domain.activity.schedule.enums.ScheduleValidData
import com.otoki.powersales.domain.activity.schedule.enums.SecondWorkType
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork3
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork5
import com.querydsl.core.types.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.LocalDate

/**
 * findScheduleList projection row — Employee/Account entity hydration 회피 (N+1 차단) 용.
 * Service layer 가 enum → displayName 변환 후 [com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleListItemDto] 로 매핑한다.
 *
 * `employeeStatus` / `employeeAppLoginActive` / `employeeEndDate` 는 재직상태 (SF formula `ValidConditionData__c`)
 * 를 Service 의 [com.otoki.powersales.domain.activity.schedule.service.internal.ScheduleDisplayStatusCalculator.employmentStatus]
 * 로 계산하기 위한 raw 입력값 (목록↔상세 동일 계산 재사용).
 */
data class ScheduleListRow(
    val id: Long,
    val employeeId: Long?,
    val employeeCode: String?,
    val employeeName: String?,
    val branchName: String?,
    val employeeStatus: String?,
    val employeeAppLoginActive: Boolean?,
    val employeeEndDate: LocalDate?,
    val accountId: Long?,
    val accountCode: String?,
    val accountName: String?,
    val accountType: String?,
    val accountStatusName: String?,
    val typeOfWork3: TypeOfWork3?,
    val typeOfWork4: SecondWorkType?,
    val typeOfWork5: TypeOfWork5?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val confirmed: Boolean?,
    val costCenterCode: String?,
    val lastMonthRevenue: BigDecimal?,
)

interface DisplayWorkScheduleRepositoryCustom {

    fun findDistinctAccountIdsByEmployeeIdAndStartDateBetween(
        employeeId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Long>

    fun findByEmployeeIdInAndNotDeleted(employeeIds: List<Long>): List<DisplayWorkSchedule>

    /**
     * SF `UplExcelBtnSchduleMasterController.checkResult` (L205) 정합 —
     * `DisplayWorkScheduleMaster__c WHERE CostCenterCode__c IN :newOrgValues AND EmployeeNumber__c IN :empCodes
     *  AND (StartDate <= :latestEndDate AND (EndDate >= :earliestStartDate OR EndDate IS NULL))`.
     *
     * BranchCodeExpander 확장 결과로 조장 지점 필터 + 사번 필터 + 입력 엑셀 행들의 최대 기간 겹침 검증.
     * `costCenterCode` 컬럼은 [DisplayWorkSchedule.costCenterCode] 직접 사용 (SF formula `CostCenterCode__c = FullName__r.CostCenterCode__c` 의 신규 직접 컬럼 매핑).
     * `employeeCode` 매칭은 `employee.employeeCode` 조인 (SF formula `EmployeeNumber__c = FullName__r.DKRetail__EmpCode__c` 와 정합).
     */
    fun findByCostCenterCodeInAndEmployeeCodeInOverlappingPeriod(
        costCenterCodes: Collection<String>,
        employeeCodes: Collection<String>,
        earliestStartDate: LocalDate,
        latestEndDate: LocalDate
    ): List<DisplayWorkSchedule>

    fun findDistinctAccountIdsByEmployeeIdAndDateRange(employeeId: Long, fromDate: LocalDate, toDate: LocalDate): List<Long>

    /**
     * @param policyPredicate SF `DisplayWorkScheduleMaster__c` 가시 범위 Predicate
     *        ([com.otoki.powersales.platform.auth.sharing.service.SharingRulePolicyEvaluator] 산출 —
     *        OWD Private → owner / role hierarchy / sharing rule(CostCenterCode 코드쌍 + CreatedById) /
     *        legacy branch OR 합성). 검색 필터와 AND 합성.
     *
     * DTO projection — Employee entity hydration 회피 (Employee.employeeInfo 가 @NotFound 로
     * 즉시 fetch 강제되어 페이지당 N+1 발생하던 사례 차단).
     *
     * `branchCodes` 는 지점 스코프(스케줄 소속 지점 `DisplayWorkSchedule.costCenterCode` IN) 필터.
     * null 이면 미적용(가시 범위 전건). 검색 필터와 AND 합성.
     *
     * `periodStart`/`periodEnd` 는 **조회 기간과 스케줄 기간의 겹침(overlap) 필터** — 시작일 자체의
     * 범위 검색이 아니라, 스케줄 기간 [startDate, endDate] 이 조회 기간과 하루라도 겹치면 매칭된다
     * (진행 중 스케줄 포함). 각각 null 이면 해당 쪽 조건 생략(열린 구간).
     */
    fun findScheduleList(
        employeeCode: String?,
        accountIds: List<Long>?,
        accountType: String?,
        accountStatus: String?,
        confirmed: Boolean?,
        typeOfWork3: String?,
        periodStart: LocalDate?,
        periodEnd: LocalDate?,
        preset: SchedulePreset?,
        validData: ScheduleValidData?,
        branchCodes: List<String>?,
        policyPredicate: Predicate,
        pageable: Pageable
    ): Page<ScheduleListRow>

    /**
     * 단건이 SF 가시 범위(`policyPredicate`) 안에 있는지 (soft-delete 제외).
     *
     * 목록과 동일한 가시 범위 Predicate 로 단건 가시성을 평가 (목록↔쓰기 경로 일관성).
     * `false` 면 호출 측에서 권한 예외 처리.
     */
    fun existsVisibleById(id: Long, policyPredicate: Predicate): Boolean

    fun findByEmployeeAndStartDate(employeeId: Long, startDate: LocalDate): List<DisplayWorkSchedule>

    fun findByEmployeeAndAccountAndStartDate(employeeId: Long, accountId: Long, startDate: LocalDate): DisplayWorkSchedule?

    fun findByEmployeeAndStartDateBetween(employeeId: Long, start: LocalDate, end: LocalDate): List<DisplayWorkSchedule>

    fun findByEmployeeIdsAndAccountIds(employeeIds: List<Long>, accountIds: List<Long>): List<DisplayWorkSchedule>

    fun findConfirmedByDateRangeAndAccountIds(monthEnd: LocalDate, monthStart: LocalDate, accountIds: List<Long>): List<DisplayWorkSchedule>

    fun existsConfirmedByEmployeeAndAccountAndDate(employeeId: Long, accountId: Long, workingDate: LocalDate): Boolean

    /**
     * 사원의 오늘 유효한 확정 진열마스터 조회
     * 조건: confirmed=true, isDeleted!=true, startDate<=date, (endDate>=date OR endDate IS NULL), employee.id=employeeId
     */
    fun findConfirmedValidByEmployeeAndDate(employeeId: Long, date: LocalDate): List<DisplayWorkSchedule>

    /**
     * 복수 사원의 오늘 유효한 확정 진열마스터 조회
     */
    fun findConfirmedValidByEmployeeIdsAndDate(employeeIds: List<Long>, date: LocalDate): List<DisplayWorkSchedule>

    /**
     * 사원의 특정 기간(from~to)과 겹치는 유효 확정 진열마스터 조회 (월간 캘린더 근무일 전개용).
     * 조건: confirmed=true, isDeleted!=true, startDate<=to, (endDate>=from OR endDate IS NULL).
     *
     * 레거시 `MyPageController.calSchedule` 의 진열 쿼리(`GENERATE_SERIES(startdate__c, enddate__c, '1 day')`)
     * 기간 전개와 정합 — 반환된 각 마스터의 [startDate, endDate] 를 호출 측에서 월 범위 내 날짜로 전개한다.
     * (홈/여사원 일별현황이 쓰는 [findConfirmedValidByEmployeeIdsAndDate] 의 기간 판정과 동일 기준.)
     */
    fun findConfirmedValidByEmployeeIdAndDateRange(employeeId: Long, from: LocalDate, to: LocalDate): List<DisplayWorkSchedule>

    /**
     * DISPLAY SAP daily batch 용 페이지 조회.
     * 조건: isDeleted!=true, confirmed=true, startDate<=date, (endDate>=date OR endDate IS NULL).
     * employee/account fetchJoin, id 오름차순.
     */
    fun findValidForDisplayMasterSapPaged(date: LocalDate, limit: Int, offset: Int): List<DisplayWorkSchedule>

    /**
     * 진열마스터 SAP **단건** 테스트 송신용 조회 (admin SAP outbound 테스트 탭).
     * `findValidForDisplayMasterSapPaged` 와 동일하게 employee/account 를 fetchJoin 하여
     * payload row 변환 시 LAZY 미초기화를 방지한다. batch 의 유효/확정/기간 필터는 적용하지 않고
     * id 로만 특정한다 — 테스트 목적상 임의 진열마스터 1건을 그대로 송신 payload 로 만들 수 있게 한다.
     * 존재하지 않으면 null.
     */
    fun findByIdForDisplayMasterSap(scheduleId: Long): DisplayWorkSchedule?

    /**
     * DISPLAY lastMonthRevenue daily batch 용 페이지 조회 (spec #690).
     *
     * legacy `UpdateLastMonthRevenueBatch.cls:7-15` SOQL `WHERE ValidData__c='유효'` 100% 동등.
     * SAP outbound batch ([findValidForDisplayMasterSapPaged]) 와의 차이: **Confirmed 조건 없음**
     * (legacy 의도된 동작 — 미확정 schedule 도 매출 갱신 대상).
     *
     * 조건: isDeleted!=true, startDate<=date, (endDate>=date OR endDate IS NULL), validDataEqualsValid(date).
     * employee/account fetchJoin, id 오름차순.
     */
    fun findValidForLastMonthRevenuePaged(date: LocalDate, limit: Int, offset: Int): List<DisplayWorkSchedule>

    /**
     * DisplayWorkSchedule 의 last_month_revenue 컬럼만 native UPDATE (spec #690).
     *
     * JPA save 회피 → BaseEntity 의 @LastModifiedDate updated_at 자동 갱신 영향 0
     * (legacy `TriggerHandler.bypass` 의 "검증/자동 set 모두 스킵, 오직 매출만 갱신" 의미 정합).
     *
     * @return 갱신 row 수 (1 이면 성공, 0 이면 id 부재)
     */
    fun updateLastMonthRevenueById(id: Long, revenue: BigDecimal): Long
}
