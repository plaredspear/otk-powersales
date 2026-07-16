package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.activity.schedule.enums.SchedulePreset
import com.otoki.powersales.domain.activity.schedule.enums.ScheduleValidData
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork3
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork5
import com.otoki.powersales.domain.foundation.account.entity.QAccount.Companion.account
import com.otoki.powersales.domain.org.employee.enums.EmploymentStatus
import com.otoki.powersales.domain.activity.schedule.entity.QDisplayWorkSchedule.Companion.displayWorkSchedule
import com.otoki.powersales.domain.org.employee.entity.QEmployee.Companion.employee
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.ComparableExpressionBase
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.StringExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.support.PageableExecutionUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

class DisplayWorkScheduleRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : DisplayWorkScheduleRepositoryCustom {

    override fun findDistinctAccountIdsByEmployeeIdAndStartDateBetween(
        employeeId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Long> {
        return queryFactory
            .select(displayWorkSchedule.account.id).distinct()
            .from(displayWorkSchedule)
            .where(
                displayWorkSchedule.employee.id.eq(employeeId),
                displayWorkSchedule.startDate.between(startDate, endDate)
            )
            .fetch()
            .filterNotNull()
    }

    override fun findDistinctAccountIdsByEmployeeIdAndDateRange(
        employeeId: Long,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<Long> {
        val dateCondition = BooleanBuilder()
            .or(displayWorkSchedule.startDate.goe(fromDate).and(displayWorkSchedule.startDate.lt(toDate)))
            .or(displayWorkSchedule.endDate.goe(fromDate).and(displayWorkSchedule.endDate.lt(toDate)))
            .or(displayWorkSchedule.endDate.isNull.and(displayWorkSchedule.startDate.lt(toDate)))

        return queryFactory
            .select(displayWorkSchedule.account.id).distinct()
            .from(displayWorkSchedule)
            .where(
                displayWorkSchedule.employee.id.eq(employeeId),
                dateCondition,
                displayWorkSchedule.account.id.isNotNull,
                isNotDeleted()
            )
            .fetch()
            .filterNotNull()
    }

    override fun findByEmployeeIdInAndNotDeleted(employeeIds: List<Long>): List<DisplayWorkSchedule> {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .where(
                displayWorkSchedule.employee.id.`in`(employeeIds),
                isNotDeleted()
            )
            .fetch()
    }

    override fun findByCostCenterCodeInAndEmployeeCodeInOverlappingPeriod(
        costCenterCodes: Collection<String>,
        employeeCodes: Collection<String>,
        earliestStartDate: LocalDate,
        latestEndDate: LocalDate
    ): List<DisplayWorkSchedule> {
        if (costCenterCodes.isEmpty() || employeeCodes.isEmpty()) return emptyList()
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .leftJoin(displayWorkSchedule.employee).fetchJoin()
            .where(
                isNotDeleted(),
                displayWorkSchedule.costCenterCode.`in`(costCenterCodes),
                displayWorkSchedule.employee.employeeCode.`in`(employeeCodes),
                displayWorkSchedule.startDate.loe(latestEndDate),
                displayWorkSchedule.endDate.goe(earliestStartDate)
                    .or(displayWorkSchedule.endDate.isNull)
            )
            .fetch()
    }

    override fun findScheduleList(
        employeeCode: String?,
        accountIds: List<Long>?,
        accountType: String?,
        accountStatus: String?,
        confirmed: Boolean?,
        typeOfWork3: String?,
        startDateFrom: LocalDate?,
        startDateTo: LocalDate?,
        preset: SchedulePreset?,
        validData: ScheduleValidData?,
        branchCodes: List<String>?,
        policyPredicate: Predicate,
        pageable: Pageable
    ): Page<ScheduleListRow> {
        val today = LocalDate.now()
        val where = BooleanBuilder()
            .and(isNotDeleted())
            .and(policyPredicate)
            .and(buildEmployeeCodeCondition(employeeCode))
            .and(buildAccountIdsCondition(accountIds))
            .and(buildAccountTypeCondition(accountType))
            .and(buildAccountStatusCondition(accountStatus))
            .and(buildConfirmedCondition(confirmed))
            .and(buildTypeOfWork3Condition(typeOfWork3))
            .and(buildStartDateFromCondition(startDateFrom))
            .and(buildStartDateToCondition(startDateTo))
            .and(buildBranchCodesCondition(branchCodes))
            .and(buildPresetCondition(preset, today))
            .and(buildValidDataCondition(validData, today))

        val content = queryFactory
            .select(
                Projections.constructor(
                    ScheduleListRow::class.java,
                    displayWorkSchedule.id,
                    employee.id,
                    employee.employeeCode,
                    employee.name,
                    employee.orgName,
                    employee.status,
                    employee.appLoginActive,
                    employee.endDate,
                    account.id,
                    account.externalKey,
                    account.name,
                    // 거래처유형 — 월매출(전산실적) 화면과 동일 축(ABC유형: abcTypeCode + abcType 결합 라벨).
                    // 기존 account.accountType(SF Account.Type, 순수 명칭) 대신 abcTypeLabel 표현식으로 통일.
                    abcTypeLabelExpr(account.abcTypeCode, account.abcType),
                    account.accountStatusName,
                    displayWorkSchedule.typeOfWork3,
                    displayWorkSchedule.typeOfWork4,
                    displayWorkSchedule.typeOfWork5,
                    displayWorkSchedule.startDate,
                    displayWorkSchedule.endDate,
                    displayWorkSchedule.confirmed,
                    displayWorkSchedule.costCenterCode,
                    displayWorkSchedule.lastMonthRevenue,
                )
            )
            .from(displayWorkSchedule)
            .leftJoin(displayWorkSchedule.employee, employee)
            .leftJoin(displayWorkSchedule.account, account)
            // policyPredicate 의 owner/hierarchy path (displayWorkSchedule.ownerUser.*) implicit
            // inner join 회피. OR 합성이라 ownerUser=null row 도 sharing rule/branch 절로 통과.
            .leftJoin(displayWorkSchedule.ownerUser)
            .where(where)
            .orderBy(*resolveOrder(pageable.sort))
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(displayWorkSchedule.count())
            .from(displayWorkSchedule)
            .leftJoin(displayWorkSchedule.ownerUser)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    /**
     * SF Formula `ValidData__c = '종료'` 의 신규 단순 매핑.
     *
     * SF formula 원본의 '종료' 분기는 사원 status (재직/퇴직/휴직) + appLoginActive + 사원 endDate + 스케줄
     * startDate/endDate 의 복합 조건이나, 본 페이지 List View "7. 종료 사원" 의 실제 운영 의미는
     * 「스케줄 종료일이 지난 레코드」 이므로 endDate < TODAY 단순 조건으로 매핑.
     */
    private fun validDataEqualsEnd(date: LocalDate): BooleanExpression =
        displayWorkSchedule.endDate.lt(date)

    /**
     * preset → WHERE 조건 변환.
     * SF List View 10개의 필터 조건 매핑 ([SchedulePreset] doc 참조).
     * BooleanBuilder 반환 — BooleanBuilder.and() chain 은 Predicate 를 리턴하므로 BooleanBuilder 로 wrap.
     */
    private fun buildPresetCondition(preset: SchedulePreset?, today: LocalDate): BooleanBuilder? {
        if (preset == null) return null
        return when (preset) {
            SchedulePreset.INPUT_TODAY ->
                BooleanBuilder(
                    displayWorkSchedule.createdAt.between(
                        today.atStartOfDay(),
                        today.atTime(LocalTime.MAX)
                    )
                )
            SchedulePreset.ALL -> null
            SchedulePreset.VALID ->
                BooleanBuilder(validDataEqualsEnd(today).not())
            SchedulePreset.VALID_CONFIRMED ->
                BooleanBuilder()
                    .and(validDataEqualsValid(today))
                    .and(displayWorkSchedule.confirmed.eq(true))
                    .and(validPeriodCondition(today))
            SchedulePreset.VALID_NOT_CONFIRMED ->
                BooleanBuilder()
                    .and(validDataEqualsValid(today))
                    .and(displayWorkSchedule.confirmed.ne(true).or(displayWorkSchedule.confirmed.isNull))
                    .and(validPeriodCondition(today))
            SchedulePreset.FIXED_VALID ->
                BooleanBuilder()
                    .and(validDataEqualsEnd(today).not())
                    .and(displayWorkSchedule.typeOfWork3.eq(TypeOfWork3.FIXED))
                    .and(displayWorkSchedule.confirmed.eq(true))
            SchedulePreset.BIFURCATION_VALID ->
                BooleanBuilder()
                    .and(validDataEqualsEnd(today).not())
                    .and(displayWorkSchedule.typeOfWork3.eq(TypeOfWork3.GAP))
                    .and(displayWorkSchedule.confirmed.eq(true))
            SchedulePreset.PATROL_VALID ->
                BooleanBuilder()
                    .and(validDataEqualsEnd(today).not())
                    .and(displayWorkSchedule.typeOfWork3.eq(TypeOfWork3.ROTATION))
                    .and(displayWorkSchedule.confirmed.eq(true))
            SchedulePreset.VALID_CONFIRMED_TEMP ->
                BooleanBuilder()
                    .and(validDataEqualsValid(today))
                    .and(displayWorkSchedule.confirmed.eq(true))
                    .and(displayWorkSchedule.typeOfWork5.eq(TypeOfWork5.TEMPORARY))
                    .and(validPeriodCondition(today))
            SchedulePreset.END ->
                BooleanBuilder(validDataEqualsEnd(today))
        }
    }

    /**
     * SF ValidData '유효' formula 의 기간 조건 절 (StartDate ≤ TODAY AND (EndDate IS NULL OR TODAY ≤ EndDate)).
     */
    private fun validPeriodCondition(date: LocalDate): BooleanExpression =
        displayWorkSchedule.startDate.loe(date)
            .and(displayWorkSchedule.endDate.goe(date).or(displayWorkSchedule.endDate.isNull))

    /**
     * 「유효여부」 조회 필터 (유효/예정/종료) → WHERE 조건 변환.
     *
     * 화면 「유효」 신호등 dot 판정 ([ScheduleDisplayStatusCalculator.validData]) 과 100% 일치하도록,
     * 동일한 복합식 (사원 status/appLoginActive/endDate + 스케줄 startDate/endDate + TODAY) 을 SQL 로 이관한다.
     * 계산기의 각 분기를 아래 boolean expression 으로 1:1 대응시킨다:
     *
     * - resigned      = status='퇴직' OR appLoginActive != true
     * - periodActive  = startDate ≤ TODAY AND (endDate IS NULL OR TODAY ≤ endDate)
     * - 유효(VALID)   = (status='재직' AND periodActive) OR (resigned AND periodActive AND empEndDate ≥ TODAY)
     * - 예정(PLANNED) = (status≠'퇴직' AND (startDate > TODAY OR (status='휴직' AND periodActive)))
     *                    OR (resigned AND empEndDate > TODAY AND startDate > TODAY)
     * - 종료(END)     = startDate IS NOT NULL AND NOT 유효 AND NOT 예정 (계산기 최종 fallthrough)
     *
     * 세 분류 모두 startDate 가 NULL 이면 계산기가 validData=null (신호등 없음) 으로 처리하므로 필터 대상에서 제외.
     * empEndDate/appLoginActive 의 NULL 은 QueryDSL 비교(.goe/.eq)가 자연스럽게 false 로 수렴 (계산기 동치).
     */
    private fun buildValidDataCondition(validData: ScheduleValidData?, today: LocalDate): BooleanBuilder? {
        if (validData == null) return null

        val startDate = displayWorkSchedule.startDate
        val endDate = displayWorkSchedule.endDate
        val empStatus = displayWorkSchedule.employee.status
        val empAppLoginActive = displayWorkSchedule.employee.appLoginActive
        val empEndDate = displayWorkSchedule.employee.endDate

        // resigned = status='퇴직' OR appLoginActive != true (NULL 포함)
        val resigned: BooleanExpression = empStatus.eq(EmploymentStatus.RESIGNED.code)
            .or(empAppLoginActive.ne(true).or(empAppLoginActive.isNull))

        // periodActive = startDate ≤ TODAY AND (endDate IS NULL OR TODAY ≤ endDate)
        val periodActive: BooleanExpression = startDate.loe(today)
            .and(endDate.isNull.or(endDate.goe(today)))

        // 유효
        val validCase: BooleanExpression = empStatus.eq(EmploymentStatus.ACTIVE.code).and(periodActive)
            .or(resigned.and(periodActive).and(empEndDate.goe(today)))

        // 예정
        val plannedCase: BooleanExpression = empStatus.ne(EmploymentStatus.RESIGNED.code)
            .and(
                startDate.gt(today)
                    .or(empStatus.eq(EmploymentStatus.ON_LEAVE.code).and(periodActive))
            )
            .or(resigned.and(empEndDate.gt(today)).and(startDate.gt(today)))

        return when (validData) {
            ScheduleValidData.VALID ->
                BooleanBuilder(startDate.isNotNull).and(validCase)
            ScheduleValidData.PLANNED ->
                // 계산기상 유효 판정이 예정보다 우선하므로 유효 케이스는 배제.
                BooleanBuilder(startDate.isNotNull).and(validCase.not()).and(plannedCase)
            ScheduleValidData.END ->
                BooleanBuilder(startDate.isNotNull).and(validCase.not()).and(plannedCase.not())
        }
    }

    /**
     * Pageable.sort 를 QueryDSL OrderSpecifier 로 변환.
     * 허용 필드: startDate, endDate, confirmed, lastMonthRevenue, createdAt.
     * 미지정 시 기본 정렬 (startDate desc, id desc) 유지.
     */
    private fun resolveOrder(sort: Sort): Array<OrderSpecifier<*>> {
        if (sort.isUnsorted) {
            return arrayOf(displayWorkSchedule.startDate.desc(), displayWorkSchedule.id.desc())
        }
        val specifiers = mutableListOf<OrderSpecifier<*>>()
        for (order in sort) {
            val path: ComparableExpressionBase<*>? = when (order.property) {
                "startDate" -> displayWorkSchedule.startDate
                "endDate" -> displayWorkSchedule.endDate
                "confirmed" -> displayWorkSchedule.confirmed
                "lastMonthRevenue" -> displayWorkSchedule.lastMonthRevenue
                "createdAt" -> displayWorkSchedule.createdAt
                else -> null
            }
            if (path != null) {
                specifiers += if (order.isAscending) path.asc() else path.desc()
            }
        }
        specifiers += displayWorkSchedule.id.desc()
        return specifiers.toTypedArray()
    }

    override fun findByEmployeeAndStartDate(employeeId: Long, startDate: LocalDate): List<DisplayWorkSchedule> {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .leftJoin(displayWorkSchedule.account).fetchJoin()
            .where(
                displayWorkSchedule.employee.id.eq(employeeId),
                displayWorkSchedule.startDate.eq(startDate)
            )
            .fetch()
    }

    override fun findByEmployeeAndAccountAndStartDate(
        employeeId: Long,
        accountId: Long,
        startDate: LocalDate
    ): DisplayWorkSchedule? {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .where(
                displayWorkSchedule.employee.id.eq(employeeId),
                displayWorkSchedule.account.id.eq(accountId),
                displayWorkSchedule.startDate.eq(startDate)
            )
            .fetchOne()
    }

    override fun findByEmployeeAndStartDateBetween(
        employeeId: Long,
        start: LocalDate,
        end: LocalDate
    ): List<DisplayWorkSchedule> {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .leftJoin(displayWorkSchedule.account).fetchJoin()
            .where(
                displayWorkSchedule.employee.id.eq(employeeId),
                displayWorkSchedule.startDate.between(start, end)
            )
            .fetch()
    }

    override fun findByEmployeeIdsAndAccountIds(
        employeeIds: List<Long>,
        accountIds: List<Long>
    ): List<DisplayWorkSchedule> {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .leftJoin(displayWorkSchedule.employee).fetchJoin()
            .leftJoin(displayWorkSchedule.account).fetchJoin()
            .where(
                displayWorkSchedule.employee.id.`in`(employeeIds),
                displayWorkSchedule.account.id.`in`(accountIds)
            )
            .fetch()
    }

    override fun findConfirmedByDateRangeAndAccountIds(
        monthEnd: LocalDate,
        monthStart: LocalDate,
        accountIds: List<Long>
    ): List<DisplayWorkSchedule> {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .where(
                displayWorkSchedule.confirmed.eq(true),
                displayWorkSchedule.startDate.loe(monthEnd),
                displayWorkSchedule.endDate.goe(monthStart),
                displayWorkSchedule.account.id.`in`(accountIds)
            )
            .fetch()
    }

    override fun existsConfirmedByEmployeeAndAccountAndDate(
        employeeId: Long,
        accountId: Long,
        workingDate: LocalDate
    ): Boolean {
        val result = queryFactory
            .selectOne()
            .from(displayWorkSchedule)
            .where(
                displayWorkSchedule.employee.id.eq(employeeId),
                displayWorkSchedule.account.id.eq(accountId),
                displayWorkSchedule.confirmed.eq(true),
                isNotDeleted(),
                displayWorkSchedule.startDate.loe(workingDate),
                displayWorkSchedule.endDate.goe(workingDate)
            )
            .fetchFirst()
        return result != null
    }

    override fun findConfirmedValidByEmployeeAndDate(
        employeeId: Long,
        date: LocalDate
    ): List<DisplayWorkSchedule> {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .leftJoin(displayWorkSchedule.account).fetchJoin()
            .where(
                displayWorkSchedule.employee.id.eq(employeeId),
                displayWorkSchedule.confirmed.eq(true),
                isNotDeleted(),
                displayWorkSchedule.startDate.loe(date),
                displayWorkSchedule.endDate.goe(date)
                    .or(displayWorkSchedule.endDate.isNull)
            )
            .fetch()
    }

    override fun findConfirmedValidByEmployeeIdsAndDate(
        employeeIds: List<Long>,
        date: LocalDate
    ): List<DisplayWorkSchedule> {
        if (employeeIds.isEmpty()) return emptyList()
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .leftJoin(displayWorkSchedule.account).fetchJoin()
            .leftJoin(displayWorkSchedule.employee).fetchJoin()
            .where(
                displayWorkSchedule.employee.id.`in`(employeeIds),
                displayWorkSchedule.confirmed.eq(true),
                isNotDeleted(),
                displayWorkSchedule.startDate.loe(date),
                displayWorkSchedule.endDate.goe(date)
                    .or(displayWorkSchedule.endDate.isNull)
            )
            .fetch()
    }

    override fun findConfirmedValidByEmployeeIdAndDateRange(
        employeeId: Long,
        from: LocalDate,
        to: LocalDate
    ): List<DisplayWorkSchedule> {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .leftJoin(displayWorkSchedule.account).fetchJoin()
            .where(
                displayWorkSchedule.employee.id.eq(employeeId),
                displayWorkSchedule.confirmed.eq(true),
                isNotDeleted(),
                displayWorkSchedule.startDate.loe(to),
                displayWorkSchedule.endDate.goe(from)
                    .or(displayWorkSchedule.endDate.isNull)
            )
            .fetch()
    }

    override fun findValidForDisplayMasterSapPaged(
        date: LocalDate,
        limit: Int,
        offset: Int
    ): List<DisplayWorkSchedule> {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .leftJoin(displayWorkSchedule.employee).fetchJoin()
            .leftJoin(displayWorkSchedule.account).fetchJoin()
            .where(
                isNotDeleted(),
                displayWorkSchedule.confirmed.eq(true),
                displayWorkSchedule.startDate.loe(date),
                displayWorkSchedule.endDate.goe(date)
                    .or(displayWorkSchedule.endDate.isNull),
                validDataEqualsValid(date)
            )
            .orderBy(displayWorkSchedule.id.asc())
            .offset(offset.toLong())
            .limit(limit.toLong())
            .fetch()
    }

    override fun findByIdForDisplayMasterSap(scheduleId: Long): DisplayWorkSchedule? {
        // findValidForDisplayMasterSapPaged 와 동일한 fetchJoin(employee/account) 으로 payload row 변환 시
        // LAZY 미초기화를 방지한다. batch 필터(유효/확정/기간)는 적용하지 않고 id 로만 특정한다.
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .leftJoin(displayWorkSchedule.employee).fetchJoin()
            .leftJoin(displayWorkSchedule.account).fetchJoin()
            .where(displayWorkSchedule.id.eq(scheduleId))
            .fetchOne()
    }

    override fun findValidForLastMonthRevenuePaged(
        date: LocalDate,
        limit: Int,
        offset: Int
    ): List<DisplayWorkSchedule> {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .leftJoin(displayWorkSchedule.employee).fetchJoin()
            .leftJoin(displayWorkSchedule.account).fetchJoin()
            .where(
                isNotDeleted(),
                displayWorkSchedule.startDate.loe(date),
                displayWorkSchedule.endDate.goe(date)
                    .or(displayWorkSchedule.endDate.isNull),
                validDataEqualsValid(date)
            )
            .orderBy(displayWorkSchedule.id.asc())
            .offset(offset.toLong())
            .limit(limit.toLong())
            .fetch()
    }

    override fun updateLastMonthRevenueById(id: Long, revenue: BigDecimal): Long {
        return queryFactory
            .update(displayWorkSchedule)
            .set(displayWorkSchedule.lastMonthRevenue, revenue)
            .where(displayWorkSchedule.id.eq(id))
            .execute()
    }

    /**
     * SF Formula `ValidData__c = '유효'` 분기의 Employee 측 동치 절 (Spec #669 Q2 재결정, 2026-05-14).
     *
     * SF formula 원본 (`_DisplayWorkScheduleMaster__c.md:116-128`) 의 '유효' 분기를 풀이하면:
     * - (status='재직') OR ((status='퇴직' OR appLoginActive=false) AND endDate>=TODAY)
     *
     * 기간 조건 (StartDate<=TODAY AND (EndDate IS NULL OR TODAY<=EndDate)) 은 호출 측 where 절의 기존
     * `startDate.loe / endDate.goe.or(isNull)` 와 중복되어 본 절에서는 제외.
     *
     * Employee.status 의 한글 값 (`재직`/`퇴직`) 비교는 [EmploymentStatus.code] 인용.
     * Employee.endDate / .appLoginActive NULL 은 `.goe(date)` / `.eq(false)` 가 자연스럽게 false 처리.
     */
    private fun validDataEqualsValid(date: LocalDate) = BooleanBuilder()
        .or(displayWorkSchedule.employee.status.eq(EmploymentStatus.ACTIVE.code))
        .or(
            BooleanBuilder()
                .and(
                    displayWorkSchedule.employee.status.eq(EmploymentStatus.RESIGNED.code)
                        .or(displayWorkSchedule.employee.appLoginActive.eq(false))
                )
                .and(displayWorkSchedule.employee.endDate.goe(date))
        )

    /**
     * 사원 검색 조건. 사번(employeeCode) 또는 이름(name) 부분 일치 중 하나라도 매칭되면 포함
     * (파라미터명은 UI 필드 유래로 employeeCode 이나, 실제 매칭은 사번+이름 겸용).
     */
    private fun buildEmployeeCodeCondition(employeeCode: String?): BooleanExpression? {
        if (employeeCode.isNullOrBlank()) return null
        val matchingIds = JPAExpressions
            .select(employee.id)
            .from(employee)
            .where(
                employee.employeeCode.containsIgnoreCase(employeeCode)
                    .or(employee.name.containsIgnoreCase(employeeCode))
            )
        return displayWorkSchedule.employee.id.`in`(matchingIds)
    }

    private fun buildAccountIdsCondition(accountIds: List<Long>?): BooleanExpression? {
        if (accountIds == null) return null
        if (accountIds.isEmpty()) return displayWorkSchedule.account.id.eq(-1) // no match
        return displayWorkSchedule.account.id.`in`(accountIds)
    }

    /**
     * 거래처유형 (ABC유형: `ABCTypeCode__c` + `ABCType__c` 결합 라벨) 정확 일치 필터.
     *
     * 월매출(전산실적) 화면과 동일 축으로 통일 — Select 옵션 값이 "1110 식품대리점_일반" 형태의
     * abcTypeLabel 결합 문자열이므로, DB 두 컬럼을 동일 규칙으로 재현한 라벨과 정확 일치(eq)로 매칭한다.
     * (기존 `Account.Type` 순수 명칭 부분일치에서 전환.)
     *
     * countQuery 가 account 를 join 하지 않으므로, [buildEmployeeCodeCondition] 와 동일하게
     * 매칭 account id 서브쿼리 IN 으로 구성 (implicit join 회피).
     */
    private fun buildAccountTypeCondition(accountType: String?): BooleanExpression? {
        if (accountType.isNullOrBlank()) return null
        val matchingIds = JPAExpressions
            .select(account.id)
            .from(account)
            .where(abcTypeLabelExpr(account.abcTypeCode, account.abcType).eq(accountType))
        return displayWorkSchedule.account.id.`in`(matchingIds)
    }

    /**
     * 거래처상태 (`AccountStatusName__c`: 거래/폐업/출고중지) 정확 일치 필터.
     *
     * 옵션 값이 서버 meta 고정 목록이므로 정확 일치(eq)로 매칭한다.
     * countQuery 가 account 를 join 하지 않으므로 [buildAccountTypeCondition] 와 동일하게
     * 매칭 account id 서브쿼리 IN 으로 구성 (implicit join 회피).
     */
    private fun buildAccountStatusCondition(accountStatus: String?): BooleanExpression? {
        if (accountStatus.isNullOrBlank()) return null
        val matchingIds = JPAExpressions
            .select(account.id)
            .from(account)
            .where(account.accountStatusName.eq(accountStatus))
        return displayWorkSchedule.account.id.`in`(matchingIds)
    }

    /**
     * 거래처유형(ABC유형) 라벨 재현 표현식 — `Account.abcTypeLabel` companion 정본과 동일 규칙.
     *
     * - 코드/명칭 둘 다 non-blank → "코드 명칭" (공백 1개 결합)
     * - 한쪽만 non-blank → 그 파트만
     * - 둘 다 blank/null → "" (Select 옵션은 blank 라벨을 만들지 않으므로 어떤 입력과도 불일치)
     *
     * 판정은 isNotBlank(trim 후 비어있지 않음)로, concat 은 원본 값으로 (정본이 trim 하지 않고 원본을 join).
     */
    private fun abcTypeLabelExpr(codeCol: StringExpression, nameCol: StringExpression): StringExpression {
        val codePresent = codeCol.isNotNull.and(codeCol.trim().ne(""))
        val namePresent = nameCol.isNotNull.and(nameCol.trim().ne(""))
        return CaseBuilder()
            .`when`(codePresent.and(namePresent)).then(codeCol.concat(" ").concat(nameCol))
            .`when`(codePresent).then(codeCol)
            .`when`(namePresent).then(nameCol)
            .otherwise("")
    }

    private fun buildConfirmedCondition(confirmed: Boolean?): BooleanExpression? {
        return confirmed?.let { displayWorkSchedule.confirmed.eq(it) }
    }

    private fun buildTypeOfWork3Condition(typeOfWork3: String?): BooleanExpression? {
        val enumValue = TypeOfWork3.fromDisplayNameOrNull(typeOfWork3) ?: return null
        return displayWorkSchedule.typeOfWork3.eq(enumValue)
    }

    private fun buildStartDateFromCondition(startDateFrom: LocalDate?): BooleanExpression? {
        return startDateFrom?.let { displayWorkSchedule.startDate.goe(it) }
    }

    private fun buildStartDateToCondition(startDateTo: LocalDate?): BooleanExpression? {
        return startDateTo?.let { displayWorkSchedule.startDate.loe(it) }
    }

    /**
     * 지점 스코프 — 스케줄 owner(조장 User)의 소속 지점(`ownerUser.costCenterCode`) IN 필터.
     * null 이면 미적용(가시 범위 전건), emptyList(NoAccess 산출값) 이면 매칭 0건(IDOR 차단).
     *
     * ## SF 정합 근거 (지점 판정 축)
     * SF `DisplayWorkScheduleMaster__c` 리스트뷰는 지점 필터 기능 자체가 없고, 조회 가시성은
     * OWD Private + Owner sharing 으로만 결정된다 (owner = 저장 시점 사원의 현재 소속 조직 조장 User,
     * SF `setOwner`). 지점명 표시(`BranchName__c` formula)도 스케줄 필드가 아니라 사원의 현재 조직명이다.
     *
     * 신규는 운영 요구로 지점 셀렉터(UI 필터)를 추가했는데, 그 필터 축을 **스케줄의 `costCenterCode`**
     * (= 저장 시점 사원 조직코드 스냅샷) 로 잡으면, 사원이 전출/발령된 뒤에도 스냅샷은 옛 조직코드로
     * 고정되어(SF `setCostCenterCode` 스냅샷 성격), 현재 지점 관리자가 지점 필터로 조회할 때 owner 는
     * 현재 지점(발령 후 조장)인데도 스케줄 스냅샷 코드가 달라 목록에서 누락되는 문제가 있었다.
     * SF 의 지점 귀속 기준(owner 조장의 소속 지점)에 맞춰 owner 의 `costCenterCode` 로 판정한다.
     * (owner_user_id 는 발령 시 재계산되어 항상 현재 조직 조장을 가리킨다 — SF `setOwner` before update.)
     */
    private fun buildBranchCodesCondition(branchCodes: List<String>?): BooleanExpression? {
        if (branchCodes == null) return null
        if (branchCodes.isEmpty()) return Expressions.FALSE.isTrue // NoAccess — 매칭 0건
        return displayWorkSchedule.ownerUser.costCenterCode.`in`(branchCodes)
    }

    override fun existsVisibleById(id: Long, policyPredicate: Predicate): Boolean {
        val where = BooleanBuilder()
            .and(displayWorkSchedule.id.eq(id))
            .and(isNotDeleted())
            .and(policyPredicate)

        return queryFactory
            .selectOne()
            .from(displayWorkSchedule)
            .leftJoin(displayWorkSchedule.ownerUser)
            .where(where)
            .fetchFirst() != null
    }

    private fun isNotDeleted(): BooleanExpression {
        return displayWorkSchedule.isDeleted.isNull.or(displayWorkSchedule.isDeleted.eq(false))
    }
}
