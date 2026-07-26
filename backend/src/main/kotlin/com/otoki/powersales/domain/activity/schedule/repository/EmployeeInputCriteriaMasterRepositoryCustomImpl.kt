package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.entity.EmployeeInputCriteriaMaster
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork1
import com.otoki.powersales.domain.foundation.account.entity.QAccountCategoryMaster.Companion.accountCategoryMaster
import com.otoki.powersales.domain.activity.schedule.entity.QEmployeeInputCriteriaMaster.Companion.employeeInputCriteriaMaster
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

    override fun findActiveByCategoryAndTypeOfWork1(
        categoryId: Long,
        typeOfWork1: TypeOfWork1,
        referenceDate: LocalDate,
    ): EmployeeInputCriteriaMaster? {
        return queryFactory
            .selectFrom(employeeInputCriteriaMaster)
            .where(
                notDeleted(),
                employeeInputCriteriaMaster.category.id.eq(categoryId),
                employeeInputCriteriaMaster.typeOfWork1.eq(typeOfWork1),
                employeeInputCriteriaMaster.confirmed.isTrue,
                employeeInputCriteriaMaster.startDate.loe(referenceDate),
                employeeInputCriteriaMaster.endDate.isNull
                    .or(employeeInputCriteriaMaster.endDate.goe(referenceDate)),
            )
            .orderBy(employeeInputCriteriaMaster.startDate.desc(), employeeInputCriteriaMaster.id.desc())
            .fetchFirst()
    }

    override fun findActiveByCategoriesAndTypeOfWork1(
        categoryIds: Collection<Long>,
        typeOfWork1: TypeOfWork1,
        referenceDate: LocalDate,
    ): Map<Long, EmployeeInputCriteriaMaster> {
        if (categoryIds.isEmpty()) return emptyMap()
        // categoryId IN 1회 조회 후 category 별 우선순위(startDate desc → id desc) 첫 row 만 취한다.
        // 단건 findActiveByCategoryAndTypeOfWork1 의 orderBy 와 동일한 정렬로 조회하므로, 각 category
        // 그룹에서 처음 만나는 row 가 단건 조회 결과와 일치한다.
        return queryFactory
            .selectFrom(employeeInputCriteriaMaster)
            .where(
                notDeleted(),
                employeeInputCriteriaMaster.category.id.`in`(categoryIds),
                employeeInputCriteriaMaster.typeOfWork1.eq(typeOfWork1),
                employeeInputCriteriaMaster.confirmed.isTrue,
                employeeInputCriteriaMaster.startDate.loe(referenceDate),
                employeeInputCriteriaMaster.endDate.isNull
                    .or(employeeInputCriteriaMaster.endDate.goe(referenceDate)),
            )
            .orderBy(employeeInputCriteriaMaster.startDate.desc(), employeeInputCriteriaMaster.id.desc())
            .fetch()
            // WHERE category.id IN (...) 로 category non-null row 만 조회되지만, entity 관계가
            // nullable 이라 안전하게 null 키를 배제한다.
            .groupBy { it.category?.id }
            .mapNotNull { (categoryId, rows) -> categoryId?.let { it to rows.first() } }
            .toMap()
    }

    private fun notDeleted() =
        employeeInputCriteriaMaster.isDeleted.isNull.or(employeeInputCriteriaMaster.isDeleted.eq(false))
}
