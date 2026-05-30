package com.otoki.powersales.inspection.repository

import com.otoki.powersales.inspection.entity.QSiteActivity.Companion.siteActivity
import com.otoki.powersales.inspection.entity.SiteActivity
import com.otoki.powersales.inspection.enums.InspectionCategory
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate

class SiteActivityRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : SiteActivityRepositoryCustom {

    override fun searchByEmployee(
        employeeId: Long,
        accountId: Int?,
        category: InspectionCategory?,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<SiteActivity> {
        val where = BooleanBuilder()
            .and(siteActivity.employee.id.eq(employeeId))
            .and(siteActivity.isDeleted.isNull.or(siteActivity.isDeleted.isFalse))
            .and(siteActivity.activityDate.goe(fromDate))
            .and(siteActivity.activityDate.loe(toDate))

        accountId?.let { where.and(siteActivity.account.id.eq(it)) }
        category?.let { where.and(siteActivity.productType.eq(it.storedValue)) }

        return queryFactory
            .selectFrom(siteActivity)
            .where(where)
            .orderBy(siteActivity.activityDate.desc(), siteActivity.id.desc())
            .fetch()
    }
}
