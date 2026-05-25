package com.otoki.powersales.auth.sharing.repository

import com.otoki.powersales.auth.sharing.entity.GroupMember
import com.otoki.powersales.auth.sharing.entity.QGroupMember.Companion.groupMember
import com.querydsl.jpa.impl.JPAQueryFactory

class GroupMemberRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : GroupMemberRepositoryCustom {

    override fun findAllByMember(targetId: Long, targetType: String): List<GroupMember> {
        return queryFactory
            .selectFrom(groupMember)
            .where(
                groupMember.userOrGroupId.eq(targetId),
                groupMember.userOrGroupType.eq(targetType),
            )
            .fetch()
    }
}
