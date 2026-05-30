package com.otoki.powersales.schedule.repository

import com.otoki.powersales.account.entity.AccountType
import com.otoki.powersales.account.entity.QAccount.Companion.account
import com.otoki.powersales.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.schedule.entity.QMonthlyFemaleEmployeeIntegrationSchedule.Companion.monthlyFemaleEmployeeIntegrationSchedule
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

        return queryFactory
            .selectFrom(mfeis)
            .leftJoin(mfeis.account, account).fetchJoin()
            .where(where)
            .orderBy(account.accountType.asc(), mfeis.workingCategory1.asc())
            .fetch()
    }
}
