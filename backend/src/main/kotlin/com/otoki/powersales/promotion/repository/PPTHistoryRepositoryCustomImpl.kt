package com.otoki.powersales.promotion.repository

import com.otoki.powersales.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamHistory
import com.otoki.powersales.promotion.entity.QProfessionalPromotionTeamHistory.Companion.professionalPromotionTeamHistory
import com.otoki.powersales.promotion.enums.ProfessionalPromotionTeamType
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import java.time.LocalDate
import java.time.LocalTime

class PPTHistoryRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : PPTHistoryRepositoryCustom {

    override fun searchHistories(
        employeeName: String?,
        employeeCode: String?,
        teamType: ProfessionalPromotionTeamType?,
        changedAtFrom: LocalDate?,
        changedAtTo: LocalDate?,
        pageable: Pageable
    ): Page<ProfessionalPromotionTeamHistory> {
        val builder = BooleanBuilder()

        if (!employeeName.isNullOrBlank()) {
            builder.and(employee.name.containsIgnoreCase(employeeName))
        }

        if (!employeeCode.isNullOrBlank()) {
            builder.and(employee.employeeCode.containsIgnoreCase(employeeCode))
        }

        if (teamType != null) {
            builder.and(professionalPromotionTeamHistory.newValue.eq(teamType))
        }

        if (changedAtFrom != null) {
            builder.and(
                professionalPromotionTeamHistory.changedAt.goe(
                    changedAtFrom.atStartOfDay()
                )
            )
        }

        if (changedAtTo != null) {
            builder.and(
                professionalPromotionTeamHistory.changedAt.loe(
                    changedAtTo.atTime(LocalTime.MAX)
                        
                )
            )
        }

        builder.and(isNotDeleted())

        val content = queryFactory
            .selectFrom(professionalPromotionTeamHistory)
            .leftJoin(professionalPromotionTeamHistory.employee, employee).fetchJoin()
            .where(builder)
            .orderBy(professionalPromotionTeamHistory.changedAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(professionalPromotionTeamHistory.count())
            .from(professionalPromotionTeamHistory)
            .leftJoin(professionalPromotionTeamHistory.employee, employee)
            .where(builder)

        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }

    private fun isNotDeleted(): BooleanExpression {
        return professionalPromotionTeamHistory.isDeleted.isNull
            .or(professionalPromotionTeamHistory.isDeleted.eq(false))
    }
}
