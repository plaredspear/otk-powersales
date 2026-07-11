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
        teamTypeGeneral: Boolean,
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

        // "일반" = 전문행사조 미지정 — 두 표현이 공존한다:
        //  (1) 신규 시스템은 미지정을 null 로 저장 (컨버터가 null 로 write).
        //  (2) SF 레거시 마이그레이션분은 문자열 '일반' 을 new_value 컬럼에 그대로 적재
        //      (SF newValue__c 는 Text 라 enum 5종 밖의 '일반' 문자열이 컨버터를 우회한 COPY 로 들어옴).
        // enum path(newValue) 로는 '일반' 문자열을 비교할 수 없어 raw 컬럼 매핑(newValueRaw)을 쓴다.
        if (teamTypeGeneral) {
            builder.and(
                professionalPromotionTeamHistory.newValueRaw.isNull
                    .or(professionalPromotionTeamHistory.newValueRaw.eq(ProfessionalPromotionTeamType.GENERAL_DISPLAY_NAME))
            )
        } else if (teamType != null) {
            // enum 의 displayName + legacyAliases 를 raw 컬럼으로 IN 매칭 — 표시명이 바뀐 유형
            // (카레세일조 ← 카레행사조)의 신·구 저장 문자열을 이력에서도 함께 조회한다.
            builder.and(professionalPromotionTeamHistory.newValueRaw.`in`(teamType.storedValues))
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
            // 1차: 사번 오름차순(null 사번은 DB 기본 NULLS LAST), 2차: 변경 시점 최근 우선.
            .orderBy(employee.employeeCode.asc(), professionalPromotionTeamHistory.changedAt.desc())
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
