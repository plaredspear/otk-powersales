package com.otoki.internal.schedule.repository

import com.otoki.internal.sap.entity.QAccount.account
import com.otoki.internal.sap.entity.QEmployee.employee
import com.otoki.internal.schedule.entity.QTeamMemberSchedule.teamMemberSchedule
import com.otoki.internal.schedule.entity.TeamMemberSchedule
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

open class TeamMemberScheduleRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : TeamMemberScheduleRepositoryCustom {

    @Transactional
    override fun updateCommuteLogId(sfid: String, commuteLogId: String) {
        queryFactory
            .update(teamMemberSchedule)
            .set(teamMemberSchedule.commuteLogId, commuteLogId)
            .where(teamMemberSchedule.sfid.eq(sfid))
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

    private fun isNotDeleted(): BooleanExpression {
        return teamMemberSchedule.isDeleted.isNull.or(teamMemberSchedule.isDeleted.eq(false))
    }

    companion object {
        const val WORKING_TYPE_ANNUAL_LEAVE = "연차"
        const val WORKING_TYPE_WORK = "근무"
    }
}
