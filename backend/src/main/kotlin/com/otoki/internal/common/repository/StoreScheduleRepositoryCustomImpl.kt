package com.otoki.internal.common.repository

import com.otoki.internal.common.entity.QStoreSchedule.storeSchedule
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate

class StoreScheduleRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : StoreScheduleRepositoryCustom {

    override fun findDistinctAccountsByFullNameAndStartDateBetween(
        fullName: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<String> {
        return queryFactory
            .select(storeSchedule.account).distinct()
            .from(storeSchedule)
            .where(
                storeSchedule.fullName.eq(fullName),
                storeSchedule.startDate.between(startDate, endDate)
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
            .select(storeSchedule.startDate).distinct()
            .from(storeSchedule)
            .where(
                storeSchedule.fullName.eq(fullName),
                storeSchedule.startDate.between(startDate, endDate)
            )
            .orderBy(storeSchedule.startDate.asc())
            .fetch()
            .filterNotNull()
    }
}
