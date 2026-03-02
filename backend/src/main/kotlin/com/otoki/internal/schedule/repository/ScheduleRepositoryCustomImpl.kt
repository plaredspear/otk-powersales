package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.QSchedule.schedule
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.transaction.annotation.Transactional

open class ScheduleRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : ScheduleRepositoryCustom {

    @Transactional
    override fun updateCommuteLogId(sfid: String, commuteLogId: String) {
        queryFactory
            .update(schedule)
            .set(schedule.commuteLogId, commuteLogId)
            .where(schedule.sfid.eq(sfid))
            .execute()
    }
}
