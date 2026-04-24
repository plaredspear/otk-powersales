package com.otoki.powersales.inspection.repository

import com.otoki.powersales.inspection.entity.InspectionTheme
import com.otoki.powersales.inspection.entity.QInspectionTheme.inspectionTheme
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate

class InspectionThemeRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : InspectionThemeRepositoryCustom {

    override fun findActiveThemesByDate(targetDate: LocalDate): List<InspectionTheme> {
        return queryFactory
            .selectFrom(inspectionTheme)
            .where(
                inspectionTheme.publicFlag.eq(true),
                inspectionTheme.startDate.loe(targetDate),
                inspectionTheme.endDate.goe(targetDate)
            )
            .orderBy(inspectionTheme.name.asc())
            .fetch()
    }
}
