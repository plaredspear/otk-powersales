package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.DisplayWorkSchedule
import com.otoki.internal.schedule.entity.QDisplayWorkSchedule.displayWorkSchedule
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate

class DisplayWorkScheduleRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : DisplayWorkScheduleRepositoryCustom {

    override fun findDistinctAccountIdsByFullNameAndStartDateBetween(
        fullName: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Int> {
        return queryFactory
            .select(displayWorkSchedule.accountId).distinct()
            .from(displayWorkSchedule)
            .where(
                displayWorkSchedule.fullName.eq(fullName),
                displayWorkSchedule.startDate.between(startDate, endDate)
            )
            .fetch()
            .filterNotNull()
    }

    override fun findDistinctStartDatesByFullNameAndDateBetween(
        fullName: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<LocalDate> {
        return queryFactory
            .select(displayWorkSchedule.startDate).distinct()
            .from(displayWorkSchedule)
            .where(
                displayWorkSchedule.fullName.eq(fullName),
                displayWorkSchedule.startDate.between(startDate, endDate)
            )
            .orderBy(displayWorkSchedule.startDate.asc())
            .fetch()
            .filterNotNull()
    }

    override fun findByFullNameInAndNotDeleted(fullNames: List<String>): List<DisplayWorkSchedule> {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .where(
                displayWorkSchedule.fullName.`in`(fullNames),
                isNotDeleted()
            )
            .fetch()
    }

    private fun isNotDeleted(): BooleanExpression {
        return displayWorkSchedule.isDeleted.isNull.or(displayWorkSchedule.isDeleted.eq(false))
    }
}
