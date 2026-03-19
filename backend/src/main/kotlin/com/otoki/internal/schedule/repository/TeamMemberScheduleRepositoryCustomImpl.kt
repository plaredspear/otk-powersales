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

    override fun findMonthlyByEmployeeNumbers(
        employeeNumbers: List<String>,
        from: LocalDate,
        to: LocalDate
    ): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .where(
                teamMemberSchedule.employeeNumber.`in`(employeeNumbers),
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

    override fun findActiveByEmployeeNumberAndDate(
        employeeNumber: String,
        workingDate: LocalDate
    ): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .where(
                teamMemberSchedule.employeeNumber.eq(employeeNumber),
                teamMemberSchedule.workingDate.eq(workingDate),
                isNotDeleted()
            )
            .fetch()
    }

    @Transactional
    override fun deleteAnnualLeaveByEmployeeNumberAndDateRange(employeeNumber: String, from: LocalDate, to: LocalDate): Long {
        return queryFactory
            .delete(teamMemberSchedule)
            .where(
                teamMemberSchedule.employeeNumber.eq(employeeNumber),
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

    override fun findAnnualLeaveByDateRangeAndEmployeeNumbers(
        from: LocalDate,
        to: LocalDate,
        employeeNumbers: List<String>
    ): List<TeamMemberSchedule> {
        return queryFactory
            .selectFrom(teamMemberSchedule)
            .where(
                teamMemberSchedule.workingDate.between(from, to),
                teamMemberSchedule.workingType.eq(WORKING_TYPE_ANNUAL_LEAVE),
                teamMemberSchedule.employeeNumber.`in`(employeeNumbers),
                isNotDeleted()
            )
            .fetch()
    }

    override fun findDistinctAccountIdsByEmployeeNumberAndDateRange(
        employeeNumber: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<Int> {
        return queryFactory
            .select(teamMemberSchedule.accountId).distinct()
            .from(teamMemberSchedule)
            .where(
                teamMemberSchedule.employeeNumber.eq(employeeNumber),
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
