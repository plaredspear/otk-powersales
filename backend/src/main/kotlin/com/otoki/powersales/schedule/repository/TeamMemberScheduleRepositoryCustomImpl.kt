package com.otoki.powersales.schedule.repository

import com.otoki.powersales.account.entity.QAccount.Companion.account
import com.otoki.powersales.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.employee.entity.QEmployeeInfo.Companion.employeeInfo
import com.otoki.powersales.schedule.entity.QTeamMemberSchedule.Companion.teamMemberSchedule
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

open class TeamMemberScheduleRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : TeamMemberScheduleRepositoryCustom {

    @Transactional
    override fun updateCommuteLogId(id: Long, commuteLogId: String) {
        queryFactory
            .update(teamMemberSchedule)
            .set(teamMemberSchedule.commuteLogId, commuteLogId)
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

    override fun findMonthlyByEmployeeIds(
        employeeIds: List<Long>,
        from: LocalDate,
        to: LocalDate
    ): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .leftJoin(teamMemberSchedule.employee, employee).fetchJoin()
            .leftJoin(employee.employeeInfo, employeeInfo).fetchJoin()
            .leftJoin(teamMemberSchedule.account, account).fetchJoin()
            .where(
                teamMemberSchedule.employee.id.`in`(employeeIds),
                teamMemberSchedule.workingDate.between(from, to),
                isNotDeleted()
            )
            .fetch()
    }

    override fun findMonthlyByAccountIds(
        accountIds: List<Int>,
        from: LocalDate,
        to: LocalDate
    ): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .leftJoin(teamMemberSchedule.employee, employee).fetchJoin()
            .leftJoin(employee.employeeInfo, employeeInfo).fetchJoin()
            .leftJoin(teamMemberSchedule.account, account).fetchJoin()
            .where(
                teamMemberSchedule.account.id.`in`(accountIds),
                teamMemberSchedule.workingDate.between(from, to),
                isNotDeleted()
            )
            .fetch()
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
    ): List<Int> {
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
                teamMemberSchedule.commuteLogId.isNotNull,
                teamMemberSchedule.account.isNotNull,
                isNotDeleted()
            )
            .fetch()
    }

    override fun findWorkSchedulesByEmployeeAndAccountAndMonth(
        employeeId: Long,
        accountId: Int,
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

    override fun countWorkSchedulesByEmployeeAndDateAndWorkingType(
        employeeId: Long,
        workingDate: LocalDate
    ): Int {
        return queryFactory
            .select(teamMemberSchedule.account.id).distinct()
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

    private fun isNotDeleted(): BooleanExpression {
        return teamMemberSchedule.isDeleted.isNull.or(teamMemberSchedule.isDeleted.eq(false))
    }

    companion object {
        const val WORKING_TYPE_ANNUAL_LEAVE = "연차"
        const val WORKING_TYPE_WORK = "근무"
    }
}
