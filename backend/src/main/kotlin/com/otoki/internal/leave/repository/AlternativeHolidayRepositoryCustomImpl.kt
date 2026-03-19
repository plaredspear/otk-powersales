package com.otoki.internal.leave.repository

import com.otoki.internal.admin.dto.response.AlternativeHolidayListItem
import com.otoki.internal.leave.entity.QAlternativeHoliday.alternativeHoliday
import com.otoki.internal.sap.entity.QUser.user
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate

class AlternativeHolidayRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : AlternativeHolidayRepositoryCustom {

    override fun findByFilters(
        startDate: LocalDate,
        endDate: LocalDate,
        status: String?,
        employeeNumber: String?,
        orgCode: String?
    ): List<AlternativeHolidayListItem> {
        val condition = BooleanBuilder()
            .and(alternativeHoliday.actualWorkDate.between(startDate, endDate))
            .and(buildStatusCondition(status))
            .and(buildEmployeeNumberCondition(employeeNumber))
            .and(buildOrgCodeCondition(orgCode))

        return queryFactory
            .select(
                Projections.constructor(
                    AlternativeHolidayListItem::class.java,
                    alternativeHoliday.id,
                    user.employeeNumber,
                    alternativeHoliday.employeeName,
                    user.orgName,
                    alternativeHoliday.actualWorkDate,
                    alternativeHoliday.targetAltHolidayDate,
                    alternativeHoliday.confirmAltHolidayDate,
                    alternativeHoliday.status,
                    alternativeHoliday.changeReason,
                    alternativeHoliday.createdBy,
                    alternativeHoliday.createdAt
                )
            )
            .from(alternativeHoliday)
            .leftJoin(user).on(user.id.eq(alternativeHoliday.employeeId))
            .where(condition)
            .orderBy(alternativeHoliday.actualWorkDate.desc(), alternativeHoliday.id.desc())
            .fetch()
    }

    private fun buildStatusCondition(status: String?) =
        status?.let { alternativeHoliday.status.eq(it) }

    private fun buildEmployeeNumberCondition(employeeNumber: String?) =
        employeeNumber?.let { user.employeeNumber.eq(it) }

    private fun buildOrgCodeCondition(orgCode: String?) =
        orgCode?.let { user.costCenterCode.eq(it) }
}
