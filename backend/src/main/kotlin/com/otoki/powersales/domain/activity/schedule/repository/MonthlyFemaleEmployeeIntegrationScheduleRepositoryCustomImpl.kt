package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.domain.foundation.account.entity.AccountType
import com.otoki.powersales.domain.foundation.account.entity.QAccount.Companion.account
import com.otoki.powersales.domain.activity.schedule.entity.QMonthlyFemaleEmployeeIntegrationSchedule.Companion.monthlyFemaleEmployeeIntegrationSchedule
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory

/**
 * 거래처유형별 환산인원 현황 보고서 Querydsl Impl (Spec #847 — 거래처유형 5종 + 대리점/대형마트 5종).
 */
class MonthlyFemaleEmployeeIntegrationScheduleRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : MonthlyFemaleEmployeeIntegrationScheduleRepositoryCustom {

    override fun findConvertedHeadcountReport(
        year: String,
        month: String,
        workingCategory5In: List<String>,
        includeNullWc5: Boolean,
        excludeConsignment: Boolean,
        costCenterCode: String?,
        accountTypeFilter: String?,
        accountTypeNotIn: List<String>,
        excludeEmpBranchName: String?,
    ): List<MonthlyFemaleEmployeeIntegrationSchedule> {
        val mfeis = monthlyFemaleEmployeeIntegrationSchedule

        val where = BooleanBuilder()
            .and(mfeis.year.eq(year))
            .and(mfeis.month.eq(month))
            // soft delete 제외 (SF 자동 제외 정합)
            .and(mfeis.isDeleted.isFalse.or(mfeis.isDeleted.isNull))

        // 근무유형5 필터 — SF multi-value equals: 선두 빈 값은 NULL/빈 포함 (includeNullWc5)
        if (workingCategory5In.isNotEmpty()) {
            val wc5 = BooleanBuilder().and(mfeis.workingCategory5.`in`(workingCategory5In))
            if (includeNullWc5) {
                wc5.or(mfeis.workingCategory5.isNull)
                    .or(mfeis.workingCategory5.eq(""))
            }
            where.and(wc5)
        }

        // 위탁농협 제외 — SF FK_$Account.ConsignmentAcc = "" (NULL + 빈 양쪽)
        if (excludeConsignment) {
            where.and(account.consignmentAcc.isNull.or(account.consignmentAcc.eq("")))
        }

        // 영업지원2팀 — CostCenterCode = 4889 (2-1)
        if (costCenterCode != null) {
            where.and(mfeis.costCenterCode.eq(costCenterCode))
        }

        // 구분(거래처유형) equals 필터 — 대리점 3종 = "대리점". SF AccountType__c = TEXT(Account.Type) 정합.
        // 대형마트(3대) 처럼 Account.Type picklist 에 없는 값은 매칭 enum 부재 → 0건 강제 (SF 죽은 필터 동작 정합).
        if (accountTypeFilter != null) {
            val type = AccountType.fromDisplayNameOrNull(accountTypeFilter)
            if (type != null) {
                where.and(account.accountType.eq(type))
            } else {
                where.and(Expressions.FALSE)
            }
        }

        // 구분(거래처유형) notIn 제외 — 2팀분리 = 대리점·백화점 제외. 매칭 enum 만 제외 (부재 displayName 은 무시).
        if (accountTypeNotIn.isNotEmpty()) {
            val excludeTypes = accountTypeNotIn.mapNotNull { AccountType.fromDisplayNameOrNull(it) }
            if (excludeTypes.isNotEmpty()) {
                where.and(account.accountType.notIn(excludeTypes))
            }
        }

        // 사원지점명(EmpBranchName) notEqual 제외 — 2팀분리 = 영업지원2팀 제외 (SF notEqual: NULL 행도 제외).
        if (excludeEmpBranchName != null) {
            where.and(mfeis.empBranchName.ne(excludeEmpBranchName))
        }

        return queryFactory
            .selectFrom(mfeis)
            .leftJoin(mfeis.account, account).fetchJoin()
            .where(where)
            .orderBy(account.accountType.asc(), mfeis.workingCategory1.asc())
            .fetch()
    }

    override fun findDeploymentDashboardRows(
        year: String,
        month: String,
        costCenterCodes: List<String>,
    ): List<MonthlyFemaleEmployeeIntegrationSchedule> {
        val mfeis = monthlyFemaleEmployeeIntegrationSchedule

        val where = BooleanBuilder()
            .and(mfeis.year.eq(year))
            .and(mfeis.month.eq(month))
            // soft delete 제외 (SF 자동 제외 정합)
            .and(mfeis.isDeleted.isFalse.or(mfeis.isDeleted.isNull))

        // 지점 스코프 — 빈 목록이면 전사 (필터 미적용)
        if (costCenterCodes.isNotEmpty()) {
            where.and(mfeis.costCenterCode.`in`(costCenterCodes))
        }

        return queryFactory
            .selectFrom(mfeis)
            .leftJoin(mfeis.account, account).fetchJoin()
            .where(where)
            .fetch()
    }
}
