package com.otoki.powersales.domain.activity.claim.repository

import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.entity.QClaim.Companion.claim
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.LocalDate

class ClaimRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : ClaimRepositoryCustom {

    override fun findOwnClaims(
        employeeId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
        accountId: Long?
    ): List<Claim> {
        val where = baseDateAccountCondition(startDate, endDate, accountId)
            .and(claim.employee.id.eq(employeeId))
        return fetchClaims(where)
    }

    override fun findCostCenterClaims(
        costCenterCode: String,
        startDate: LocalDate,
        endDate: LocalDate,
        accountId: Long?
    ): List<Claim> {
        val where = baseDateAccountCondition(startDate, endDate, accountId)
            .and(claim.costCenterCode.eq(costCenterCode))
        return fetchClaims(where)
    }

    /**
     * 두 목록 쿼리 공통 조건: 발생일자([Claim.date]) BETWEEN + 거래처([accountId]) 옵션 필터.
     * `accountId` 가 null 이면 거래처 조건을 추가하지 않아 전체를 포함한다(레거시 `:accountId IS NULL OR ...` 동등).
     */
    private fun baseDateAccountCondition(
        startDate: LocalDate,
        endDate: LocalDate,
        accountId: Long?
    ): BooleanBuilder {
        return BooleanBuilder()
            .and(claim.date.between(startDate, endDate))
            .and(accountId?.let { claim.account.id.eq(it) })
    }

    private fun fetchClaims(where: BooleanBuilder): List<Claim> {
        return queryFactory
            .selectFrom(claim)
            .where(where)
            .orderBy(claim.date.desc(), claim.createdAt.desc())
            .fetch()
    }
}
