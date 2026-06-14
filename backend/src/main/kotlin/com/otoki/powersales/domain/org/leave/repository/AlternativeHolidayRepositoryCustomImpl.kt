package com.otoki.powersales.domain.org.leave.repository

import com.otoki.powersales.domain.org.leave.dto.response.AlternativeHolidayListItem
import com.otoki.powersales.domain.org.leave.entity.QAlternativeHoliday.Companion.alternativeHoliday
import com.otoki.powersales.domain.org.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.domain.org.leave.enums.AltHolidayStatus
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
        employeeCode: String?,
        orgCode: String?
    ): List<AlternativeHolidayListItem> {
        val condition = BooleanBuilder()
            .and(alternativeHoliday.actualWorkDate.between(startDate, endDate))
            .and(buildStatusCondition(status))
            .and(buildEmployeeCodeCondition(employeeCode))
            .and(buildOrgCodeCondition(orgCode))

        return queryFactory
            .select(
                Projections.constructor(
                    AlternativeHolidayListItem::class.java,
                    alternativeHoliday.id,
                    employee.employeeCode,
                    employee.name,
                    employee.orgName,
                    alternativeHoliday.actualWorkDate,
                    alternativeHoliday.targetAltHolidayDate,
                    alternativeHoliday.confirmAltHolidayDate,
                    alternativeHoliday.status,
                    alternativeHoliday.changeReason,
                    alternativeHoliday.createdByEmpNo,
                    alternativeHoliday.createdAt
                )
            )
            .from(alternativeHoliday)
            .leftJoin(employee).on(employee.id.eq(alternativeHoliday.employeeId))
            .where(condition)
            .orderBy(alternativeHoliday.actualWorkDate.desc(), alternativeHoliday.id.desc())
            .fetch()
    }

    private fun buildStatusCondition(status: String?) =
        status?.let {
            AltHolidayStatus.fromDisplayNameOrNull(it)
                ?.let { altStatus -> alternativeHoliday.status.eq(altStatus) }
        }

    private fun buildEmployeeCodeCondition(employeeCode: String?) =
        employeeCode?.let { employee.employeeCode.eq(it) }

    private fun buildOrgCodeCondition(orgCode: String?) =
        orgCode?.let { employee.costCenterCode.eq(it) }
}
