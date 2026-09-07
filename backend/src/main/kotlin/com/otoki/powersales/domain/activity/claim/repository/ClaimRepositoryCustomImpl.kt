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
     * 두 목록 쿼리 공통 조건: 등록일시([Claim.createdAt]) 기간 + 거래처([accountId]) 옵션 필터.
     *
     * 기간 축은 **등록일시**다 — 레거시 SF `IF_REST_MOBILE_ClaimSearch.cls` 가
     * `where CreatedDate >= :startDate and CreatedDate <= :endDate.addDays(1)` 로 조회하고
     * 응답의 ClaimDate 자리에도 CreatedDate 를 실어 보낸다. 발생일자(`Claim.date`, SF ClaimDate) 는
     * 웹 등록분에서 등록일과 다를 수 있어 기간 축으로 쓰면 레거시와 결과가 갈린다.
     * 종료일은 레거시의 `addDays(1)` 와 동등하게 다음날 0시 미만으로 연다.
     *
     * `accountId` 가 null 이면 거래처 조건을 추가하지 않아 전체를 포함한다(레거시 `:accountId IS NULL OR ...` 동등).
     */
    private fun baseDateAccountCondition(
        startDate: LocalDate,
        endDate: LocalDate,
        accountId: Long?
    ): BooleanBuilder {
        return BooleanBuilder()
            .and(claim.createdAt.goe(startDate.atStartOfDay()))
            .and(claim.createdAt.lt(endDate.plusDays(1).atStartOfDay()))
            .and(accountId?.let { claim.account.id.eq(it) })
    }

    private fun fetchClaims(where: BooleanBuilder): List<Claim> {
        return queryFactory
            .selectFrom(claim)
            .where(where)
            .orderBy(claim.createdAt.desc())
            .fetch()
    }
}
