package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.entity.MonthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.domain.foundation.account.entity.QAccount.Companion.account
import com.otoki.powersales.domain.activity.schedule.entity.QMonthlyFemaleEmployeeIntegrationSchedule.Companion.monthlyFemaleEmployeeIntegrationSchedule
import com.otoki.powersales.domain.org.employee.entity.QEmployee.Companion.employee
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory

/**
 * 거래처유형별 환산인원 현황 보고서 Querydsl Impl (Spec #847 — 거래처유형 5종 + 대리점/대형마트 5종).
 */
class MonthlyFemaleEmployeeIntegrationScheduleRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : MonthlyFemaleEmployeeIntegrationScheduleRepositoryCustom {

    override fun findByIdWithEmployeeAndAccount(id: Long): MonthlyFemaleEmployeeIntegrationSchedule? {
        val mfeis = monthlyFemaleEmployeeIntegrationSchedule
        return queryFactory
            .selectFrom(mfeis)
            .leftJoin(mfeis.employee, employee).fetchJoin()
            .leftJoin(mfeis.account, account).fetchJoin()
            .where(mfeis.id.eq(id))
            .fetchOne()
    }

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
        branchScopeCodes: List<String>,
    ): List<MonthlyFemaleEmployeeIntegrationSchedule> {
        val mfeis = monthlyFemaleEmployeeIntegrationSchedule

        val where = BooleanBuilder()
            .and(mfeis.year.eq(year))
            .and(mfeis.month.eq(month))
            // soft delete 제외 (SF 자동 제외 정합)
            .and(mfeis.isDeleted.isFalse.or(mfeis.isDeleted.isNull))

        // 지점 스코프 — 여사원 소속 지점(costCenterCode) IN. 빈 목록이면 전사(미적용).
        // 소속기준 variant 만 채워 넘어오며, 2팀 단일 costCenterCode.eq 필터와 AND 로 공존한다.
        if (branchScopeCodes.isNotEmpty()) {
            where.and(mfeis.costCenterCode.`in`(branchScopeCodes))
        }

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
        // accountType 은 거래처유형마스터 Name(raw String) 을 그대로 보관하므로 직접 eq 비교한다.
        if (accountTypeFilter != null) {
            where.and(account.accountType.eq(accountTypeFilter))
        }

        // 구분(거래처유형) notIn 제외 — 2팀분리 = 대리점·백화점 제외. raw String 직접 notIn.
        if (accountTypeNotIn.isNotEmpty()) {
            where.and(account.accountType.notIn(accountTypeNotIn))
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
    ): List<DashboardDeploymentRow> {
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

        // entity/account 전 컬럼 대신 집계가 쓰는 7개 필드만 select (account fetch join 80여 컬럼 회피).
        // account 미연결 row 는 left join 으로 account* 가 null.
        return queryFactory
            .select(
                Projections.constructor(
                    DashboardDeploymentRow::class.java,
                    mfeis.convertedHeadcount,
                    mfeis.workingCategory1,
                    mfeis.workingCategory3,
                    account.id,
                    account.externalKey,
                    account.accountType,
                ),
            )
            .from(mfeis)
            .leftJoin(mfeis.account, account)
            .where(where)
            .fetch()
    }
}
