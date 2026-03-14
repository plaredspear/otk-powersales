package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.QDisplayWorkSchedule.displayWorkSchedule
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate

class DisplayWorkScheduleRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : DisplayWorkScheduleRepositoryCustom {

    override fun findDistinctAccountsByFullNameAndStartDateBetween(
        fullName: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<String> {
        return queryFactory
            .select(displayWorkSchedule.account).distinct()
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
}
