package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamHistory
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.foundation.account.entity.QAccount.Companion.account
import com.otoki.powersales.domain.org.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.domain.activity.promotion.entity.QProfessionalPromotionTeamHistory.Companion.professionalPromotionTeamHistory
import com.otoki.powersales.domain.activity.promotion.entity.QProfessionalPromotionTeamMaster.Companion.professionalPromotionTeamMaster
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
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
        branchCodeFilter: List<String>?,
        pageable: Pageable
    ): Page<PPTHistorySearchResult> {
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

        // 지점 스코프 — 데이터의 branch_code 컬럼(빈값)이 아니라 사원 소속 지점(costCenterCode) 기준.
        // 전문행사조 마스터 조회와 동일 정책.
        if (!branchCodeFilter.isNullOrEmpty()) {
            builder.and(employee.costCenterCode.`in`(branchCodeFilter))
        }

        builder.and(isNotDeleted())

        return fetchHistoryPage(builder, pageable)
    }

    override fun findHistoriesByEmployeeId(employeeId: Long, pageable: Pageable): Page<PPTHistorySearchResult> {
        val builder = BooleanBuilder()
        builder.and(professionalPromotionTeamHistory.employeeId.eq(employeeId))
        builder.and(isNotDeleted())

        return fetchHistoryPage(builder, pageable)
    }

    /**
     * 이력 + 사원 컨텍스트 + 원인 마스터(masterId) 거래처 projection 을 공통 조회한다.
     *
     * 이력 → 원인 마스터(masterId) → 거래처(account) 를 left join 하여, masterId 가 null 인 이력은
     * master/account 조인이 매칭되지 않아 거래처 두 필드가 null 이 된다. 정렬은 변경 시점 역순.
     */
    private fun fetchHistoryPage(builder: BooleanBuilder, pageable: Pageable): Page<PPTHistorySearchResult> {
        val content = queryFactory
            .select(
                Projections.constructor(
                    PPTHistorySearchResult::class.java,
                    professionalPromotionTeamHistory,
                    employee.name,
                    employee.employeeCode,
                    employee.orgName,
                    account.externalKey,
                    account.name,
                )
            )
            .from(professionalPromotionTeamHistory)
            .leftJoin(employee).on(professionalPromotionTeamHistory.employeeId.eq(employee.id))
            .leftJoin(professionalPromotionTeamMaster)
            .on(professionalPromotionTeamHistory.masterId.eq(professionalPromotionTeamMaster.id))
            .leftJoin(account).on(professionalPromotionTeamMaster.accountId.eq(account.id))
            .where(builder)
            .orderBy(professionalPromotionTeamHistory.changedAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(professionalPromotionTeamHistory.count())
            .from(professionalPromotionTeamHistory)
            .leftJoin(employee).on(professionalPromotionTeamHistory.employeeId.eq(employee.id))
            .where(builder)

        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }

    private fun isNotDeleted(): BooleanExpression {
        return professionalPromotionTeamHistory.isDeleted.isNull
            .or(professionalPromotionTeamHistory.isDeleted.eq(false))
    }
}
