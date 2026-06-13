package com.otoki.powersales.claim.repository

import com.otoki.powersales.domain.foundation.account.entity.QAccount.Companion.account
import com.otoki.powersales.claim.entity.Claim
import com.otoki.powersales.claim.enums.ClaimStatus
import com.otoki.powersales.claim.enums.ClaimType1
import com.otoki.powersales.claim.entity.QClaim.Companion.claim
import com.otoki.powersales.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.product.entity.QProduct.Companion.product
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import java.time.LocalDate
import java.time.LocalDateTime

class AdminClaimRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : AdminClaimRepositoryCustom {

    override fun findClaims(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        status: ClaimStatus?,
        employeeName: String?,
        storeName: String?,
        pageable: Pageable
    ): Page<Claim> {
        val where = BooleanBuilder()
            .and(buildDateRangeCondition(startDateTime, endDateTime))
            .and(buildStatusCondition(status))
            .and(buildEmployeeNameCondition(employeeName))
            .and(buildStoreNameCondition(storeName))

        val content = queryFactory
            .selectFrom(claim)
            .leftJoin(claim.employee, employee).fetchJoin()
            .leftJoin(claim.account, account).fetchJoin()
            .leftJoin(claim.product, product).fetchJoin()
            .where(where)
            .orderBy(claim.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(claim.count())
            .from(claim)
            .leftJoin(claim.employee, employee)
            .leftJoin(claim.account, account)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    override fun findPeriodReport(
        startDate: LocalDate,
        endDate: LocalDate,
        claimType1: ClaimType1?,
    ): List<Claim> {
        return queryFactory
            .selectFrom(claim)
            .leftJoin(claim.employee, employee).fetchJoin()
            .leftJoin(claim.account, account).fetchJoin()
            .leftJoin(claim.product, product).fetchJoin()
            .where(
                // ClaimDate 기준 기간 (목록 화면의 createdAt 과 다름 — SF dateColumn=ClaimDate__c)
                claim.date.between(startDate, endDate),
                // SAP 전송 완료 건만 (레거시 Status = 전송완료)
                claim.status.eq(ClaimStatus.SENT),
                // PACKAGING → claimType1=A, ALL → 필터 없음 (A/B/C 전체)
                claimType1?.let { claim.claimType1.eq(it) },
            )
            .orderBy(claim.date.desc())
            .fetch()
    }

    private fun buildDateRangeCondition(startDateTime: LocalDateTime, endDateTime: LocalDateTime): Predicate {
        return claim.createdAt.between(startDateTime, endDateTime)
    }

    private fun buildStatusCondition(status: ClaimStatus?): Predicate? {
        return status?.let { claim.status.eq(it) }
    }

    private fun buildEmployeeNameCondition(employeeName: String?): Predicate? {
        if (employeeName.isNullOrBlank()) return null
        return employee.name.like("%${employeeName}%")
    }

    private fun buildStoreNameCondition(storeName: String?): Predicate? {
        if (storeName.isNullOrBlank()) return null
        return account.name.like("%${storeName}%")
    }
}
