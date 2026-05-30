package com.otoki.powersales.promotion.repository

import com.otoki.powersales.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.promotion.enums.ProfessionalPromotionTeamType
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
        teamType: ProfessionalPromotionTeamType?,
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

        if (teamType != null) {
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

    override fun findConfirmedReport(): List<ProfessionalPromotionTeamMaster> {
        return queryFactory
            .selectFrom(professionalPromotionTeamMaster)
            .leftJoin(professionalPromotionTeamMaster.employee, employee).fetchJoin()
            .leftJoin(professionalPromotionTeamMaster.account, account).fetchJoin()
            .where(
                // 확정 인원만 (SF Confirmed__c = 1)
                professionalPromotionTeamMaster.isConfirmed.isTrue,
                // soft-delete 제외
                professionalPromotionTeamMaster.isDeleted.isNull
                    .or(professionalPromotionTeamMaster.isDeleted.isFalse),
                // 전사 — SF scope=organization (지점 스코프 없음)
            )
            .orderBy(professionalPromotionTeamMaster.branchCode.asc())
            .fetch()
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
        teamType: ProfessionalPromotionTeamType,
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

    /**
     * 전문행사조 마스터 SAP 송신 대상 조회 (Spec #765 — UC-15).
     *
     * 당월 기간 (`monthFirstDay` ≤ start_date ≤ end_date 또는 end_date IS NULL) 과 겹치는 모든 마스터를 반환한다.
     * 레거시 `IF_REST_SAP_PPTMToSAP.cls:22-36` SOQL 의 실효 동작 1:1 정합 — 레거시는 `ValidConditionData != '미확정'` 필터가
     * 있었으나 수식 결과값이 `'미확정'` 을 생성하지 않으므로 본 조건은 항상 참 (작성자가 `ValidData != '미확정'` 을 의도한
     * 오타로 추정). 확정/미확정 마스터 모두 송신된다 (운영 실측 동등).
     */
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
                    .or(professionalPromotionTeamMaster.endDate.goe(monthFirstDay))
            )
            .orderBy(professionalPromotionTeamMaster.id.asc())
            .fetch()
    }
}
