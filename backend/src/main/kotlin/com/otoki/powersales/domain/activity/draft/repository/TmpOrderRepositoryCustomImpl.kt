package com.otoki.powersales.domain.activity.draft.repository

import com.otoki.powersales.domain.activity.draft.entity.TmpOrder
import com.otoki.powersales.domain.activity.draft.entity.QTmpOrder.Companion.tmpOrder
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.LockModeType

class TmpOrderRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : TmpOrderRepositoryCustom {

    override fun findByEmployeeIdForUpdate(employeeId: Long): TmpOrder? {
        return queryFactory
            .selectFrom(tmpOrder)
            .where(tmpOrder.employeeId.eq(employeeId))
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .fetchOne()
    }
}
