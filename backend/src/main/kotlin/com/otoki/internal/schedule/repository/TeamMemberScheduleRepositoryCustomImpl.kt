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
        employeeIds: List<String>,
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
        accountIds: List<String>,
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
        employeeId: String,
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

    private fun isNotDeleted(): BooleanExpression {
        return teamMemberSchedule.isDeleted.isNull.or(teamMemberSchedule.isDeleted.eq(false))
    }
}
