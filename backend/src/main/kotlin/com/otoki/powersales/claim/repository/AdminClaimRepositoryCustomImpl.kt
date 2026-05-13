package com.otoki.powersales.claim.repository

import com.otoki.powersales.claim.entity.Claim
import com.otoki.powersales.claim.entity.ClaimStatus
import com.otoki.powersales.claim.entity.QClaim.Companion.claim
import com.otoki.powersales.employee.entity.QEmployee.Companion.employee
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
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
            .where(where)
            .orderBy(claim.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(claim.count())
            .from(claim)
            .leftJoin(claim.employee, employee)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
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
        return claim.accountName.like("%${storeName}%")
    }
}
