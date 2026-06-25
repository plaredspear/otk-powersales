package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamMaster
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.activity.promotion.entity.QProfessionalPromotionTeamMaster.Companion.professionalPromotionTeamMaster
import com.otoki.powersales.domain.foundation.account.entity.QAccount.Companion.account
import com.otoki.powersales.domain.org.employee.entity.QEmployee.Companion.employee
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.core.types.dsl.StringExpression
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
        branchCodeFilter: List<String>?,
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

        // 지점 스코프 — 데이터의 branch_code 컬럼(빈값)이 아니라 사원 소속 지점(costCenterCode) 기준.
        if (!branchCodeFilter.isNullOrEmpty()) {
            builder.and(employee.costCenterCode.`in`(branchCodeFilter))
        }

        if (validOnly) {
            // SF `ValidData__c = '유효'` 동등. ValidData__c formula 는 Confirmed__c==false → '미확정' 으로
            // 분기하므로 '유효' 는 확정(isConfirmed=true) 마스터만 포함한다 (findValidMasters 와 동일 전제).
            builder.and(
                professionalPromotionTeamMaster.isConfirmed.isTrue
                    .and(professionalPromotionTeamMaster.startDate.loe(today))
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
                    account.name,
                    employee.orgName,
                    employee.status,
                    employee.appLoginActive,
                    employee.endDate,
                    account.accountType
                )
            )
            .from(professionalPromotionTeamMaster)
            .leftJoin(employee).on(professionalPromotionTeamMaster.employeeId.eq(employee.id))
            .leftJoin(account).on(professionalPromotionTeamMaster.accountId.eq(account.id))
            .where(builder)
            .orderBy(
                professionalPromotionTeamMaster.createdAt.desc(),
                professionalPromotionTeamMaster.id.desc()
            )
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
        // legacy `Batch_PPTMaster1` 의 WHERE ValidData__c='유효' 동등.
        // ValidData__c formula 는 Confirmed__c==false → '미확정' 으로 분기하므로 '유효' 는 확정 마스터만 포함한다.
        // → 미확정(isConfirmed=false, ex: bulk 업로드) 마스터는 sync 대상에서 제외해야 한다.
        return queryFactory
            .selectFrom(professionalPromotionTeamMaster)
            .where(
                professionalPromotionTeamMaster.isConfirmed.isTrue,
                professionalPromotionTeamMaster.startDate.loe(today),
                professionalPromotionTeamMaster.endDate.isNull
                    .or(professionalPromotionTeamMaster.endDate.goe(today))
            )
            .fetch()
    }

    override fun findExpiringMasters(today: LocalDate): List<ProfessionalPromotionTeamMaster> {
        // legacy `Batch_PPTMaster2` 의 WHERE ValidData__c='유효' AND EndDate__c=TODAY 동등.
        // ValidData__c formula 는 Confirmed__c==false → '미확정' 으로 분기하므로 '유효' 는 확정 마스터만 포함한다.
        // → 미확정(isConfirmed=false) 마스터는 종료일이 오늘이어도 만료 대상에서 제외 (sync 와 동일 전제).
        return queryFactory
            .selectFrom(professionalPromotionTeamMaster)
            .where(
                professionalPromotionTeamMaster.isConfirmed.isTrue,
                professionalPromotionTeamMaster.endDate.eq(today)
            )
            .fetch()
    }

    override fun findValidMastersByEmployeeIdAndTeamType(
        employeeId: Long,
        accountId: Long,
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
     * 레거시 `IF_REST_SAP_PPTMToSAP.cls:33-35` SOQL 의 WHERE 3조건을 1:1 재현한다:
     *   1. `StartDate__c <= :lastDay`
     *   2. `(EndDate__c >= :firstDay OR EndDate__c = null)`
     *   3. `ValidConditionData__c != '미확정'` — [validConditionDataNotUnconfirmed] 로 재현.
     *
     * 3번은 레거시에서 **dead filter** (항상 참) 다. `ValidConditionData__c` 수식의 산출값은
     * `퇴직날짜`/`퇴직예정날짜`/`휴직`/`재직` 4종뿐이라 `'미확정'` 을 절대 생성하지 않기 때문 (작성자가
     * `ValidData__c != '미확정'` 을 의도한 오타로 추정). 그럼에도 **레거시 SOQL 구조 동등성** 을 위해
     * 동일 조건을 명시적으로 재현한다 — 결과 행 집합은 조건 유무와 무관하게 동일하며, 확정/미확정 마스터
     * 모두 송신된다 (운영 실측 동등). 향후 레거시 수식이 '미확정' 을 산출하도록 바뀌면 본 절이 자동 반영된다.
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
                    .or(professionalPromotionTeamMaster.endDate.goe(monthFirstDay)),
                validConditionDataNotUnconfirmed()
            )
            .orderBy(professionalPromotionTeamMaster.id.asc())
            .fetch()
    }

    /**
     * 레거시 SOQL 의 `ValidConditionData__c != '미확정'` 절을 재현한다.
     *
     * 레거시 `ValidConditionData__c` 수식 (`PPTMasterPayloadFactory.computeValidConditionData` 와 동일)은
     * 사원 재직상태 기반으로 `퇴직YYYY-MM-DD`/`퇴직예정YYYY-MM-DD`/`휴직`/`재직` 만 산출하고 `'미확정'` 은
     * 산출하지 않는다. 따라서 그 수식을 CASE 로 재현한 결과는 어떤 행에서도 `'미확정'` 이 되지 않아 본 절은
     * 항상 참(dead filter) 이다. SOQL 구조 동등성을 위해 수식 자체를 SQL 로 풀어 `!= '미확정'` 을 적용한다.
     */
    private fun validConditionDataNotUnconfirmed(): BooleanExpression {
        val today = LocalDate.now()
        val isResigned = employee.status.eq("퇴직").or(employee.appLoginActive.isFalse)
        val validConditionData: StringExpression = CaseBuilder()
            .`when`(isResigned.and(employee.endDate.isNotNull).and(employee.endDate.lt(today)))
            .then(Expressions.stringTemplate("concat('퇴직', {0})", employee.endDate.stringValue()))
            .`when`(isResigned.and(employee.endDate.isNotNull).and(employee.endDate.gt(today)))
            .then(Expressions.stringTemplate("concat('퇴직예정', {0})", employee.endDate.stringValue()))
            .`when`(employee.status.eq("휴직"))
            .then("휴직")
            .otherwise("재직")
        return validConditionData.ne("미확정")
    }
}
