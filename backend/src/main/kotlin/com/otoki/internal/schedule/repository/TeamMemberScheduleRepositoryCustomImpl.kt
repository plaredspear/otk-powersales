package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.QTeamMemberSchedule.teamMemberSchedule
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.transaction.annotation.Transactional

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
}
