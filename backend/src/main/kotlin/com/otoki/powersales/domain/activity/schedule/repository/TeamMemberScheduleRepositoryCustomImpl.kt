package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.dto.response.DailySummaryDto
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.enums.AttendanceType
import com.otoki.powersales.domain.activity.schedule.sap.AttendanceSapPayloadRow
import com.otoki.powersales.domain.foundation.account.entity.QAccount.Companion.account
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.domain.activity.schedule.entity.QAttendanceLog.Companion.attendanceLog
import com.otoki.powersales.domain.activity.schedule.entity.QTeamMemberSchedule.Companion.teamMemberSchedule
import com.otoki.powersales.user.entity.QUser.Companion.user
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.NumberExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

open class TeamMemberScheduleRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : TeamMemberScheduleRepositoryCustom {

    @Transactional
    override fun updateAttendanceLog(id: Long, attendanceLogId: Long) {
        queryFactory
            .update(teamMemberSchedule)
            .set(teamMemberSchedule.attendanceLog.id, attendanceLogId)
            .where(teamMemberSchedule.id.eq(id))
            .execute()
    }

    @Transactional
    override fun updateSafetyCheckData(
        id: Long,
        equipment1: String?,
        equipment2: String?,
        equipment3: String?,
        equipment4: String?,
        equipment5: String?,
        equipment6: String?,
        equipment7: String?,
        equipment8: String?,
        equipment9: String?,
        yesChkCnt: Double?,
        noChkCnt: Double?,
        startTime: LocalDateTime?,
        completeTime: LocalDateTime?,
        precaution: String?,
        precautionChk: Double?,
        traversalFlag: String?
    ) {
        queryFactory
            .update(teamMemberSchedule)
            .set(teamMemberSchedule.equipment1, equipment1)
            .set(teamMemberSchedule.equipment2, equipment2)
            .set(teamMemberSchedule.equipment3, equipment3)
            .set(teamMemberSchedule.equipment4, equipment4)
            .set(teamMemberSchedule.equipment5, equipment5)
            .set(teamMemberSchedule.equipment6, equipment6)
            .set(teamMemberSchedule.equipment7, equipment7)
            .set(teamMemberSchedule.equipment8, equipment8)
            .set(teamMemberSchedule.equipment9, equipment9)
            .set(teamMemberSchedule.yesChkCnt, yesChkCnt)
            .set(teamMemberSchedule.noChkCnt, noChkCnt)
            .set(teamMemberSchedule.startTime, startTime)
            .set(teamMemberSchedule.completeTime, completeTime)
            .set(teamMemberSchedule.precaution, precaution)
            .set(teamMemberSchedule.precautionChk, precautionChk)
            .set(teamMemberSchedule.traversalFlag, traversalFlag)
            .where(teamMemberSchedule.id.eq(id))
            .execute()
    }

    override fun findByEmployeeIdAndWorkingDate(
        employeeId: Long,
        workingDate: LocalDate
    ): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .leftJoin(teamMemberSchedule.account, account).fetchJoin()
            .where(
                teamMemberSchedule.employee.id.eq(employeeId),
                teamMemberSchedule.workingDate.eq(workingDate),
                isNotDeleted()
            )
            .fetch()
    }

    override fun findDailyStatusByEmployeeIds(
        date: LocalDate,
        employeeIds: List<Long>
    ): List<TeamMemberSchedule> {
        if (employeeIds.isEmpty()) return emptyList()
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .leftJoin(teamMemberSchedule.employee, employee).fetchJoin()
            .leftJoin(teamMemberSchedule.account, account).fetchJoin()
            .leftJoin(teamMemberSchedule.attendanceLog, attendanceLog).fetchJoin()
            .where(
                teamMemberSchedule.workingDate.eq(date),
                employee.id.`in`(employeeIds),
                isNotDeleted()
            )
            .fetch()
    }

    override fun findMonthlyByEmployeeIds(
        employeeIds: List<Long>,
        from: LocalDate,
        to: LocalDate,
        promotionTeams: List<String>?
    ): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .leftJoin(teamMemberSchedule.employee, employee).fetchJoin()
            // employee.employeeInfo(공유 PK @MapsId @OneToOne) fetch join 제거:
            // root 가 TMS 라 한 사원이 한 달치 다행으로 반복 + readOnly tx 에서 loadedState 미보관 →
            // 2번째 행부터 EntityInitializer.updateInitializedEntityInstance 가 NPE(loadedState null).
            // 호출부(getMonthlySchedule/AdminTeamScheduleService)는 employeeInfo 를 읽지 않아 안전하게 제거.
            .leftJoin(teamMemberSchedule.account, account).fetchJoin()
            .where(
                teamMemberSchedule.employee.id.`in`(employeeIds),
                teamMemberSchedule.workingDate.between(from, to),
                professionalPromotionTeamIn(promotionTeams),
                isNotDeleted()
            )
            .fetch()
    }

    override fun findMonthlyByAccountIds(
        accountIds: List<Long>,
        from: LocalDate,
        to: LocalDate,
        promotionTeams: List<String>?
    ): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .leftJoin(teamMemberSchedule.employee, employee).fetchJoin()
            // employee.employeeInfo fetch join 제거 (findMonthlyByEmployeeIds 와 동일 사유 — loadedState NPE 회피)
            .leftJoin(teamMemberSchedule.account, account).fetchJoin()
            .where(
                teamMemberSchedule.account.id.`in`(accountIds),
                teamMemberSchedule.workingDate.between(from, to),
                professionalPromotionTeamIn(promotionTeams),
                isNotDeleted()
            )
            .fetch()
    }

    private fun professionalPromotionTeamIn(teams: List<String>?): BooleanExpression? {
        return if (teams.isNullOrEmpty()) null
        else teamMemberSchedule.professionalPromotionTeam.`in`(teams)
    }

    override fun aggregateDailySummaryByEmployeeIds(
        employeeIds: List<Long>,
        from: LocalDate,
        to: LocalDate
    ): List<DailySummaryDto> {
        if (employeeIds.isEmpty()) return emptyList()

        // buildDailySummary 의 JVM 산식과 동일한 분류 — 조건별 1/0 케이스를 SUM 하여 DB 에서 집계.
        val isWork = teamMemberSchedule.workingType.eq(WorkingType.WORK)
        val isEvent = teamMemberSchedule.workingCategory1.eq(WorkingCategory1.EVENT)
        val hasAttendance = teamMemberSchedule.attendanceLog.isNotNull

        val displayExpected = countWhere(isWork.and(isEvent.not().or(teamMemberSchedule.workingCategory1.isNull)))
        val displayActual = countWhere(
            isWork.and(isEvent.not().or(teamMemberSchedule.workingCategory1.isNull)).and(hasAttendance)
        )
        val promotionExpected = countWhere(isWork.and(isEvent))
        val promotionActual = countWhere(isWork.and(isEvent).and(hasAttendance))
        val annualLeave = countWhere(teamMemberSchedule.workingType.eq(WorkingType.ANNUAL_LEAVE))
        val compensatoryLeave = countWhere(teamMemberSchedule.workingType.eq(WorkingType.ALT_HOLIDAY))

        return queryFactory
            .select(
                Projections.constructor(
                    DailySummaryDto::class.java,
                    teamMemberSchedule.workingDate.stringValue(),
                    displayExpected,
                    displayActual,
                    promotionExpected,
                    promotionActual,
                    annualLeave,
                    compensatoryLeave
                )
            )
            .from(teamMemberSchedule)
            .where(
                teamMemberSchedule.employee.id.`in`(employeeIds),
                teamMemberSchedule.workingDate.between(from, to),
                isNotDeleted()
            )
            .groupBy(teamMemberSchedule.workingDate)
            .orderBy(teamMemberSchedule.workingDate.asc())
            .fetch()
    }

    // 조건 충족 행을 1, 그 외 0 으로 환산한 뒤 합산 — 조건부 COUNT 를 Integer 합으로 표현.
    // then(Int)/otherwise(Int) 오버로드가 NumberExpression<Int> 를 반환해야 public sum() 이 적용된다.
    private fun countWhere(condition: BooleanExpression): NumberExpression<Int> {
        val oneIfMatched: NumberExpression<Int> = CaseBuilder()
            .`when`(condition).then(1)
            .otherwise(0)
        return oneIfMatched.sumAggregate()
    }

    override fun findActiveByEmployeeIdAndDate(
        employeeId: Long,
        workingDate: LocalDate
    ): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .leftJoin(teamMemberSchedule.account, account).fetchJoin()
            .where(
                teamMemberSchedule.employee.id.eq(employeeId),
                teamMemberSchedule.workingDate.eq(workingDate),
                isNotDeleted()
            )
            .fetch()
    }

    @Transactional
    override fun deleteAnnualLeaveByEmployeeIdAndDateRange(employeeId: Long, from: LocalDate, to: LocalDate): Long {
        return queryFactory
            .delete(teamMemberSchedule)
            .where(
                teamMemberSchedule.employee.id.eq(employeeId),
                teamMemberSchedule.workingDate.between(from, to),
                teamMemberSchedule.workingType.eq(WORKING_TYPE_ANNUAL_LEAVE)
            )
            .execute()
    }

    @Transactional
    override fun deleteFutureWorkSchedulesByEmployeeId(employeeId: Long, fromDate: LocalDate): Long {
        return queryFactory
            .delete(teamMemberSchedule)
            .where(
                teamMemberSchedule.employee.id.eq(employeeId),
                teamMemberSchedule.workingDate.gt(fromDate),
                teamMemberSchedule.workingType.eq(WORKING_TYPE_WORK),
                isNotDeleted()
            )
            .execute()
    }

    override fun findAnnualLeaveByDateRange(from: LocalDate, to: LocalDate): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .leftJoin(teamMemberSchedule.employee, employee).fetchJoin()
            .where(
                teamMemberSchedule.workingDate.between(from, to),
                teamMemberSchedule.workingType.eq(WORKING_TYPE_ANNUAL_LEAVE),
                isNotDeleted()
            )
            .fetch()
    }

    override fun findAnnualLeaveByDateRangeAndEmployeeIds(
        from: LocalDate,
        to: LocalDate,
        employeeIds: List<Long>
    ): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .leftJoin(teamMemberSchedule.employee, employee).fetchJoin()
            .where(
                teamMemberSchedule.workingDate.between(from, to),
                teamMemberSchedule.workingType.eq(WORKING_TYPE_ANNUAL_LEAVE),
                teamMemberSchedule.employee.id.`in`(employeeIds),
                isNotDeleted()
            )
            .fetch()
    }

    override fun findDistinctAccountIdsByEmployeeIdAndDateRange(
        employeeId: Long,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<Long> {
        return queryFactory
            .select(teamMemberSchedule.account.id).distinct()
            .from(teamMemberSchedule)
            .where(
                teamMemberSchedule.employee.id.eq(employeeId),
                teamMemberSchedule.workingDate.goe(fromDate),
                teamMemberSchedule.workingDate.lt(toDate),
                teamMemberSchedule.account.isNotNull,
                isNotDeleted()
            )
            .fetch()
            .filterNotNull()
    }

    override fun findDistinctScheduledAccounts(
        keyword: String?,
        limit: Int
    ): List<Account> {
        // JOIN + DISTINCT 는 LIMIT 가 무용지물(전체 176만 TMS 스캔 → 70여 컬럼 DISTINCT/SORT 후에야 절단).
        // account 를 name 순으로 훑으며 EXISTS 세미조인으로 TMS 를 account_id 로 probe(LIMIT 즉시 단락) —
        // V207 idx_team_member_schedule_account_id_working_date (account_id 선두) 인덱스가 probe 를 가속.
        return queryFactory
            .selectFrom(account)
            .where(
                account.isDeleted.isNull.or(account.isDeleted.eq(false)),
                accountKeywordMatch(keyword),
                JPAExpressions.selectOne()
                    .from(teamMemberSchedule)
                    .where(
                        teamMemberSchedule.account.eq(account),
                        isNotDeleted()
                    )
                    .exists()
            )
            .orderBy(account.name.asc())
            .limit(limit.toLong())
            .fetch()
    }

    private fun accountKeywordMatch(keyword: String?): BooleanExpression? {
        if (keyword.isNullOrBlank()) return null
        return account.name.containsIgnoreCase(keyword)
            .or(account.externalKey.containsIgnoreCase(keyword))
    }

    override fun findDistinctAccountIdsByTeamLeaderIdAndDateRange(
        teamLeaderId: Long,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<Long> {
        return queryFactory
            .select(teamMemberSchedule.account.id).distinct()
            .from(teamMemberSchedule)
            .where(
                teamMemberSchedule.teamLeader.id.eq(teamLeaderId),
                teamMemberSchedule.workingDate.goe(fromDate),
                teamMemberSchedule.workingDate.lt(toDate),
                teamMemberSchedule.account.isNotNull,
                isNotDeleted()
            )
            .fetch()
            .filterNotNull()
    }

    override fun findIntegrationScheduleRecords(
        employeeIds: List<Long>,
        from: LocalDate,
        to: LocalDate
    ): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .leftJoin(teamMemberSchedule.employee, employee).fetchJoin()
            .leftJoin(teamMemberSchedule.account, account).fetchJoin()
            .where(
                teamMemberSchedule.employee.id.`in`(employeeIds),
                teamMemberSchedule.workingDate.between(from, to),
                teamMemberSchedule.workingType.eq(WORKING_TYPE_WORK),
                // Spec #789 — 출근 등록 여부 가드를 id-FK 로 전환 (sfid 비즈니스 로직 사용 금지 정책 정합).
                teamMemberSchedule.attendanceLog.isNotNull,
                teamMemberSchedule.account.isNotNull,
                isNotDeleted()
            )
            .fetch()
    }

    override fun findWorkSchedulesByEmployeeAndAccountAndMonth(
        employeeId: Long,
        accountId: Long,
        from: LocalDate,
        to: LocalDate
    ): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .leftJoin(teamMemberSchedule.employee, employee).fetchJoin()
            .leftJoin(teamMemberSchedule.account, account).fetchJoin()
            .where(
                teamMemberSchedule.employee.id.eq(employeeId),
                teamMemberSchedule.account.id.eq(accountId),
                teamMemberSchedule.workingDate.between(from, to),
                teamMemberSchedule.workingType.eq(WORKING_TYPE_WORK),
                isNotDeleted()
            )
            .fetch()
    }

    override fun countWorkScheduleRowsByEmployeeAndDate(
        employeeId: Long,
        workingDate: LocalDate
    ): Int {
        return queryFactory
            .select(teamMemberSchedule.id)
            .from(teamMemberSchedule)
            .where(
                teamMemberSchedule.employee.id.eq(employeeId),
                teamMemberSchedule.workingDate.eq(workingDate),
                teamMemberSchedule.workingType.eq(WORKING_TYPE_WORK),
                teamMemberSchedule.account.isNotNull,
                isNotDeleted()
            )
            .fetch()
            .size
    }

    override fun countDistinctWorkingDatesByEmployeeAndCostCenterAndMonth(
        employeeId: Long,
        costCenterCode: String,
        from: LocalDate,
        to: LocalDate
    ): Int {
        return queryFactory
            .select(teamMemberSchedule.workingDate).distinct()
            .from(teamMemberSchedule)
            .where(
                teamMemberSchedule.employee.id.eq(employeeId),
                teamMemberSchedule.costCenterCode.eq(costCenterCode),
                teamMemberSchedule.workingDate.between(from, to),
                teamMemberSchedule.workingType.eq(WORKING_TYPE_WORK),
                isNotDeleted()
            )
            .fetch()
            .size
    }

    override fun findRegularAttendancesForSapPaged(
        today: LocalDate,
        yesterday: LocalDate,
        limit: Int,
        offset: Int
    ): List<AttendanceSapPayloadRow> {
        return queryFactory
            .select(
                Projections.constructor(
                    AttendanceSapPayloadRow::class.java,
                    attendanceLog.id,
                    teamMemberSchedule.workingDate,
                    employee.employeeCode,
                    account.externalKey,
                    teamMemberSchedule.workingCategory1,
                    teamMemberSchedule.workingCategory2,
                    teamMemberSchedule.workingCategory3,
                    attendanceLog.secondWorkType
                )
            )
            .from(teamMemberSchedule)
            // Spec #789 — sfid 양방향 매칭 (#587 P1-B §1.4) → id-FK 단방향 JOIN (#672 audit 정합). attendance_log_id 는 V103 신설, V176 backfill.
            .join(attendanceLog).on(teamMemberSchedule.attendanceLog.id.eq(attendanceLog.id))
            .join(employee).on(employee.id.eq(attendanceLog.employeeId))
            .join(account).on(account.id.eq(attendanceLog.accountId))
            .where(
                attendanceLog.attendanceType.eq(AttendanceType.REGULAR),
                teamMemberSchedule.workingType.eq(WORKING_TYPE_WORK),
                teamMemberSchedule.workingDate.`in`(today, yesterday)
            )
            .orderBy(attendanceLog.id.asc())
            .offset(offset.toLong())
            .limit(limit.toLong())
            .fetch()
    }

    override fun findPlacementCheck(
        from: LocalDate,
        to: LocalDate,
        roles: List<String>,
        branchCodes: List<String>,
    ): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .join(teamMemberSchedule.employee, employee).fetchJoin()
            .leftJoin(teamMemberSchedule.account, account).fetchJoin()
            .where(
                teamMemberSchedule.workingDate.between(from, to),
                teamMemberSchedule.workingType.eq(WORKING_TYPE_WORK),
                employee.role.`in`(roles),
                employee.name.notLike("%테스트용%"),
                employee.name.notLike("%관리자%"),
                employee.name.notLike("%파워세일즈%"),
                // 퇴직자 포함 (status 필터 없음) — soft-delete 사원만 제외 (Spec #839 Q2)
                employee.isDeleted.isNull.or(employee.isDeleted.eq(false)),
                costCenterCodeIn(branchCodes),
                isNotDeleted(),
            )
            .fetch()
    }

    override fun findWorkHistory(
        employeeCode: String,
        from: LocalDate,
        to: LocalDate,
        branchCodes: List<String>,
    ): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .join(teamMemberSchedule.employee, employee).fetchJoin()
            .leftJoin(teamMemberSchedule.account, account).fetchJoin()
            .where(
                employee.employeeCode.eq(employeeCode),
                teamMemberSchedule.workingDate.between(from, to),
                costCenterCodeIn(branchCodes),
                isNotDeleted(),
            )
            .orderBy(teamMemberSchedule.workingDate.asc())
            .fetch()
    }

    override fun findSafetyCheckReport(
        date: LocalDate,
        branchCodes: List<String>,
    ): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .join(teamMemberSchedule.employee, employee).fetchJoin()
            .leftJoin(teamMemberSchedule.account, account).fetchJoin()
            .where(
                teamMemberSchedule.workingDate.eq(date),
                // 순회/점검 대상 (레거시 TraversalFlag = ',O')
                teamMemberSchedule.traversalFlag.eq("O"),
                // 점검 응답 존재 = 점검 완료 (레거시 Yes_ChkCnt != 빈값)
                teamMemberSchedule.yesChkCnt.isNotNull,
                costCenterCodeIn(branchCodes),
                isNotDeleted(),
            )
            .orderBy(teamMemberSchedule.workingCategory1.asc())
            .fetch()
    }

    override fun findSafetyCheckReportRpa(
        date: LocalDate,
    ): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .join(teamMemberSchedule.employee, employee).fetchJoin()
            .leftJoin(teamMemberSchedule.account, account).fetchJoin()
            // CUST_NAME 컬럼 — SF Report 의사 컬럼 = 레코드 Owner. ownerUser 조인.
            .leftJoin(teamMemberSchedule.ownerUser, user).fetchJoin()
            .where(
                teamMemberSchedule.workingDate.eq(date),
                // 순회/점검 대상 (레거시 TraversalFlag = ',O')
                teamMemberSchedule.traversalFlag.eq("O"),
                // 점검 응답 존재 = 점검 완료 (레거시 Yes_ChkCnt != 빈값)
                teamMemberSchedule.yesChkCnt.isNotNull,
                // 전사 고정 — SF scope=organization (지점 스코프 없음)
                isNotDeleted(),
            )
            .orderBy(teamMemberSchedule.workingCategory1.asc())
            .fetch()
    }

    /**
     * DataScope 지점 스코프 — 사원 소속 지점(costCenterCode) 기준 (Spec #839/#840 Q3).
     * SF `CurrentUserBranchNameList` + 여사원 일정관리(`ScheduleSearchByTeamMemberController`) 정합 —
     * 거래처 소재 지점(account.branchCode) 이 아니라 일정의 costCenterCode 로 필터.
     */
    private fun costCenterCodeIn(branchCodes: List<String>): BooleanExpression? {
        return if (branchCodes.isEmpty()) null
        else teamMemberSchedule.costCenterCode.`in`(branchCodes)
    }

    private fun isNotDeleted(): BooleanExpression {
        return teamMemberSchedule.isDeleted.isNull.or(teamMemberSchedule.isDeleted.eq(false))
    }

    companion object {
        val WORKING_TYPE_ANNUAL_LEAVE = WorkingType.ANNUAL_LEAVE
        val WORKING_TYPE_WORK = WorkingType.WORK
    }
}
