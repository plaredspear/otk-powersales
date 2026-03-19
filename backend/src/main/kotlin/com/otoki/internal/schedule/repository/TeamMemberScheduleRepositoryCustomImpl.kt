package com.otoki.internal.schedule.repository

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

    override fun findMonthlyByEmployeeIds(
        employeeIds: List<Long>,
        from: LocalDate,
        to: LocalDate
    ): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .where(
                teamMemberSchedule.employeeId.`in`(employeeIds),
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
            .where(
                teamMemberSchedule.accountId.`in`(accountIds),
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
            .where(
                teamMemberSchedule.employeeId.eq(employeeId),
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
                teamMemberSchedule.employeeId.eq(employeeId),
                teamMemberSchedule.workingDate.between(from, to),
                teamMemberSchedule.workingType.eq(WORKING_TYPE_ANNUAL_LEAVE)
            )
            .execute()
    }

    override fun findAnnualLeaveByDateRange(from: LocalDate, to: LocalDate): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
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
            .where(
                teamMemberSchedule.workingDate.between(from, to),
                teamMemberSchedule.workingType.eq(WORKING_TYPE_ANNUAL_LEAVE),
                teamMemberSchedule.employeeId.`in`(employeeIds),
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
            .select(teamMemberSchedule.accountId).distinct()
            .from(teamMemberSchedule)
            .where(
                teamMemberSchedule.employeeId.eq(employeeId),
                teamMemberSchedule.workingDate.goe(fromDate),
                teamMemberSchedule.workingDate.lt(toDate),
                teamMemberSchedule.accountId.isNotNull,
                isNotDeleted()
            )
            .fetch()
            .filterNotNull()
    }

    private fun isNotDeleted(): BooleanExpression {
        return teamMemberSchedule.isDeleted.isNull.or(teamMemberSchedule.isDeleted.eq(false))
    }

    companion object {
        const val WORKING_TYPE_ANNUAL_LEAVE = "연차"
    }
}
