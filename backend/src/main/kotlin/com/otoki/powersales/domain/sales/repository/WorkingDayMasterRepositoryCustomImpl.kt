package com.otoki.powersales.domain.sales.repository

import com.otoki.powersales.domain.sales.entity.QWorkingDayMaster.Companion.workingDayMaster
import com.otoki.powersales.domain.sales.entity.WorkingDayMaster
import com.otoki.powersales.user.entity.QUser
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate

class WorkingDayMasterRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : WorkingDayMasterRepositoryCustom {

    // is_deleted 는 nullable(SF migration row 정합)이라 NULL 도 미삭제로 통과시킨다 (SF SOQL 자동 제외 동등).
    private val notDeleted: BooleanExpression =
        workingDayMaster.isDeleted.isNull.or(workingDayMaster.isDeleted.isFalse)

    override fun countWorkingDays(start: LocalDate, end: LocalDate, check: Double): Long {
        return queryFactory
            .select(workingDayMaster.count())
            .from(workingDayMaster)
            .where(
                workingDayMaster.workingDate.goe(start),
                workingDayMaster.workingDate.loe(end),
                workingDayMaster.workingDateCheck.eq(check),
                notDeleted,
            )
            .fetchOne() ?: 0L
    }

    override fun findByWorkingDateRange(start: LocalDate, end: LocalDate): List<WorkingDayMaster> {
        // createdBy / lastModifiedBy 를 각각 fetch join 하려면 서로 다른 QUser alias 가 필요하다.
        val createdByUser = QUser("createdByUser")
        val lastModifiedByUser = QUser("lastModifiedByUser")
        return queryFactory
            .selectFrom(workingDayMaster)
            .leftJoin(workingDayMaster.createdBy, createdByUser).fetchJoin()
            .leftJoin(workingDayMaster.lastModifiedBy, lastModifiedByUser).fetchJoin()
            .where(
                workingDayMaster.workingDate.goe(start),
                workingDayMaster.workingDate.loe(end),
                notDeleted,
            )
            .orderBy(workingDayMaster.workingDate.asc())
            .fetch()
    }
}
