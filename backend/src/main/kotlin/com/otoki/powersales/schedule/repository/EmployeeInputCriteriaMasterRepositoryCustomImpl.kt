package com.otoki.powersales.schedule.repository

import com.otoki.powersales.account.entity.QAccountCategoryMaster.Companion.accountCategoryMaster
import com.otoki.powersales.schedule.entity.EmployeeInputCriteriaMaster
import com.otoki.powersales.schedule.entity.QEmployeeInputCriteriaMaster.Companion.employeeInputCriteriaMaster
import com.otoki.powersales.schedule.enums.TypeOfWork1
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate

class EmployeeInputCriteriaMasterRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : EmployeeInputCriteriaMasterRepositoryCustom {

    override fun findAllNotDeleted(): List<EmployeeInputCriteriaMaster> {
        return queryFactory
            .selectFrom(employeeInputCriteriaMaster)
            .leftJoin(employeeInputCriteriaMaster.category, accountCategoryMaster).fetchJoin()
            .where(notDeleted())
            .orderBy(employeeInputCriteriaMaster.startDate.desc(), employeeInputCriteriaMaster.id.desc())
            .fetch()
    }

    override fun existsOverlapping(
        categoryId: Long,
        typeOfWork1: TypeOfWork1?,
        startDate: LocalDate,
        endDate: LocalDate?,
        excludeId: Long,
    ): Boolean {
        val typeOfWork1Match = if (typeOfWork1 == null) {
            employeeInputCriteriaMaster.typeOfWork1.isNull
        } else {
            employeeInputCriteriaMaster.typeOfWork1.eq(typeOfWork1)
        }

        val endDateMatch = if (endDate == null) {
            null
        } else {
            employeeInputCriteriaMaster.startDate.loe(endDate)
        }

        val builder = BooleanBuilder()
            .and(notDeleted())
            .and(employeeInputCriteriaMaster.category.id.eq(categoryId))
            .and(typeOfWork1Match)
            .and(employeeInputCriteriaMaster.id.ne(excludeId))
            .and(
                employeeInputCriteriaMaster.endDate.isNull
                    .or(employeeInputCriteriaMaster.endDate.goe(startDate))
            )

        if (endDateMatch != null) builder.and(endDateMatch)

        val exists = queryFactory
            .selectOne()
            .from(employeeInputCriteriaMaster)
            .where(builder)
            .fetchFirst()

        return exists != null
    }

    private fun notDeleted() =
        employeeInputCriteriaMaster.isDeleted.isNull.or(employeeInputCriteriaMaster.isDeleted.eq(false))
}
