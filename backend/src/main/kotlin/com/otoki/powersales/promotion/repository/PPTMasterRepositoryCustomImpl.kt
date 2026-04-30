package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.promotion.entity.QProfessionalPromotionTeamMaster.Companion.professionalPromotionTeamMaster
import com.otoki.powersales.account.entity.QAccount.Companion.account
import com.otoki.powersales.employee.entity.QEmployee.Companion.employee
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import java.time.LocalDate

class PPTMasterRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : PPTMasterRepositoryCustom {

    override fun searchMasters(
        employeeName: String?,
        employeeCode: String?,
        teamType: String?,
        branchCode: String?,
        validOnly: Boolean,
        today: LocalDate,
        pageable: Pageable
    ): Page<PPTMasterSearchResult> {
        val builder = BooleanBuilder()

        if (!employeeName.isNullOrBlank()) {
            builder.and(employee.name.containsIgnoreCase(employeeName))
        }

        if (!employeeCode.isNullOrBlank()) {
            builder.and(employee.employeeCode.eq(employeeCode))
        }

        if (!teamType.isNullOrBlank()) {
            builder.and(professionalPromotionTeamMaster.teamType.eq(teamType))
        }

        if (!branchCode.isNullOrBlank()) {
            builder.and(professionalPromotionTeamMaster.branchCode.eq(branchCode))
        }

        if (validOnly) {
            builder.and(
                professionalPromotionTeamMaster.startDate.loe(today)
                    .and(
                        professionalPromotionTeamMaster.endDate.isNull
                            .or(professionalPromotionTeamMaster.endDate.goe(today))
                    )
            )
        }

        val content = queryFactory
            .select(
                Projections.constructor(
                    PPTMasterSearchResult::class.java,
                    professionalPromotionTeamMaster,
                    employee.employeeCode,
                    employee.name,
                    account.externalKey,
                    account.name
                )
            )
            .from(professionalPromotionTeamMaster)
            .leftJoin(employee).on(professionalPromotionTeamMaster.employeeId.eq(employee.id))
            .leftJoin(account).on(professionalPromotionTeamMaster.accountId.eq(account.id))
            .where(builder)
            .orderBy(professionalPromotionTeamMaster.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(professionalPromotionTeamMaster.count())
            .from(professionalPromotionTeamMaster)
            .leftJoin(employee).on(professionalPromotionTeamMaster.employeeId.eq(employee.id))
            .leftJoin(account).on(professionalPromotionTeamMaster.accountId.eq(account.id))
            .where(builder)

        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }

    override fun findValidMasters(today: LocalDate): List<ProfessionalPromotionTeamMaster> {
        return queryFactory
            .selectFrom(professionalPromotionTeamMaster)
            .where(
                professionalPromotionTeamMaster.startDate.loe(today),
                professionalPromotionTeamMaster.endDate.isNull
                    .or(professionalPromotionTeamMaster.endDate.goe(today))
            )
            .fetch()
    }

    override fun findExpiringMasters(today: LocalDate): List<ProfessionalPromotionTeamMaster> {
        return queryFactory
            .selectFrom(professionalPromotionTeamMaster)
            .where(professionalPromotionTeamMaster.endDate.eq(today))
            .fetch()
    }

    override fun findValidMastersByEmployeeIdAndTeamType(
        employeeId: Long,
        accountId: Int,
        teamType: String,
        startDate: LocalDate,
        excludeId: Long?
    ): List<ProfessionalPromotionTeamMaster> {
        val builder = BooleanBuilder()
        builder.and(professionalPromotionTeamMaster.employeeId.eq(employeeId))
        builder.and(professionalPromotionTeamMaster.accountId.eq(accountId))
        builder.and(professionalPromotionTeamMaster.teamType.eq(teamType))
        builder.and(
            professionalPromotionTeamMaster.endDate.isNull
                .or(professionalPromotionTeamMaster.endDate.goe(startDate))
        )
        if (excludeId != null) {
            builder.and(professionalPromotionTeamMaster.id.ne(excludeId))
        }

        return queryFactory
            .selectFrom(professionalPromotionTeamMaster)
            .where(builder)
            .fetch()
    }

    override fun findValidMastersByEmployeeId(employeeId: Long, today: LocalDate): List<ProfessionalPromotionTeamMaster> {
        return queryFactory
            .selectFrom(professionalPromotionTeamMaster)
            .where(
                professionalPromotionTeamMaster.employeeId.eq(employeeId),
                professionalPromotionTeamMaster.startDate.loe(today),
                professionalPromotionTeamMaster.endDate.isNull
                    .or(professionalPromotionTeamMaster.endDate.goe(today))
            )
            .fetch()
    }

    override fun findSapOutboundTargets(
        monthFirstDay: LocalDate,
        monthLastDay: LocalDate
    ): List<ProfessionalPromotionTeamMaster> {
        return queryFactory
            .selectFrom(professionalPromotionTeamMaster)
            .leftJoin(professionalPromotionTeamMaster.employee, employee).fetchJoin()
            .leftJoin(professionalPromotionTeamMaster.account, account).fetchJoin()
            .where(
                professionalPromotionTeamMaster.startDate.loe(monthLastDay),
                professionalPromotionTeamMaster.endDate.isNull
                    .or(professionalPromotionTeamMaster.endDate.goe(monthFirstDay)),
                employee.status.isNull.or(employee.status.ne("미확정"))
            )
            .orderBy(professionalPromotionTeamMaster.id.asc())
            .fetch()
    }
}
