package com.otoki.powersales.domain.activity.productexpiration.repository

import com.otoki.powersales.domain.activity.productexpiration.dto.response.AdminProductExpirationSummaryResponse
import com.otoki.powersales.domain.activity.productexpiration.entity.ProductExpiration
import com.otoki.powersales.domain.activity.productexpiration.entity.QProductExpiration.Companion.productExpiration
import com.otoki.powersales.domain.org.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.domain.org.employee.entity.QEmployeeInfo.Companion.employeeInfo
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import java.time.LocalDate

class ProductExpirationRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : ProductExpirationRepositoryCustom {

    override fun findForAdmin(
        fromDate: LocalDate?,
        toDate: LocalDate?,
        employeeKeyword: String?,
        accountKeyword: String?,
        status: String?,
        today: LocalDate,
        pageable: Pageable,
        employeeIds: List<Long>?
    ): Page<ProductExpiration> {
        val where = BooleanBuilder()
            .and(buildDateRangeCondition(fromDate, toDate))
            .and(buildEmployeeKeywordCondition(employeeKeyword))
            .and(buildAccountKeywordCondition(accountKeyword))
            .and(buildStatusCondition(status, today))
            .and(buildEmployeeIdsCondition(employeeIds))

        val content = queryFactory
            .selectFrom(productExpiration)
            .leftJoin(productExpiration.employee, employee).fetchJoin()
            .where(where)
            .orderBy(productExpiration.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(productExpiration.count())
            .from(productExpiration)
            .leftJoin(productExpiration.employee, employee)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    override fun getSummary(today: LocalDate, employeeIds: List<Long>?): AdminProductExpirationSummaryResponse {
        val sevenDaysLater = today.plusDays(7)

        val expiredCount = CaseBuilder()
            .`when`(productExpiration.expirationDate.isNull.or(productExpiration.expirationDate.loe(today)))
            .then(1L).otherwise(0L)

        val imminentCount = CaseBuilder()
            .`when`(productExpiration.expirationDate.gt(today).and(productExpiration.expirationDate.loe(sevenDaysLater)))
            .then(1L).otherwise(0L)

        val normalCount = CaseBuilder()
            .`when`(productExpiration.expirationDate.gt(sevenDaysLater))
            .then(1L).otherwise(0L)

        val where = BooleanBuilder()
            .and(buildEmployeeIdsCondition(employeeIds))

        val result = queryFactory
            .select(
                productExpiration.count(),
                expiredCount.sumAggregate(),
                imminentCount.sumAggregate(),
                normalCount.sumAggregate()
            )
            .from(productExpiration)
            .where(where)
            .fetchOne()

        return AdminProductExpirationSummaryResponse(
            totalCount = result?.get(0, Long::class.java) ?: 0L,
            expiredCount = result?.get(1, Long::class.java) ?: 0L,
            imminentCount = result?.get(2, Long::class.java) ?: 0L,
            normalCount = result?.get(3, Long::class.java) ?: 0L
        )
    }

    override fun findDistinctFcmTokensByAlarmDate(alarmDate: LocalDate): List<String> {
        return queryFactory
            .select(employeeInfo.fcmToken)
            .distinct()
            .from(productExpiration)
            .join(productExpiration.employee, employee)
            .join(employee.employeeInfo, employeeInfo)
            .where(
                productExpiration.alarmDate.eq(alarmDate),
                employeeInfo.fcmToken.isNotNull,
                employeeInfo.fcmToken.ne(""),
            )
            .fetch()
    }

    private fun buildDateRangeCondition(fromDate: LocalDate?, toDate: LocalDate?): Predicate? {
        if (fromDate == null && toDate == null) return null
        if (fromDate != null && toDate != null) return productExpiration.expirationDate.between(fromDate, toDate)
        if (fromDate != null) return productExpiration.expirationDate.goe(fromDate)
        return productExpiration.expirationDate.loe(toDate!!)
    }

    private fun buildEmployeeKeywordCondition(keyword: String?): Predicate? {
        if (keyword.isNullOrBlank()) return null
        val pattern = "%${keyword.lowercase()}%"
        return employee.name.lower().like(pattern)
            .or(employee.employeeCode.lower().like(pattern))
    }

    private fun buildAccountKeywordCondition(keyword: String?): Predicate? {
        if (keyword.isNullOrBlank()) return null
        val pattern = "%${keyword.lowercase()}%"
        return productExpiration.accountName.lower().like(pattern)
            .or(productExpiration.accountCode.lower().like(pattern))
    }

    private fun buildEmployeeIdsCondition(employeeIds: List<Long>?): Predicate? {
        if (employeeIds == null) return null
        if (employeeIds.isEmpty()) return productExpiration.employeeId.isNull.and(productExpiration.employeeId.isNotNull)
        return productExpiration.employeeId.`in`(employeeIds)
    }

    private fun buildStatusCondition(status: String?, today: LocalDate): Predicate? {
        if (status.isNullOrBlank()) return null
        val sevenDaysLater = today.plusDays(7)
        return when (status.uppercase()) {
            "EXPIRED" -> productExpiration.expirationDate.isNull.or(productExpiration.expirationDate.loe(today))
            "IMMINENT" -> productExpiration.expirationDate.gt(today).and(productExpiration.expirationDate.loe(sevenDaysLater))
            "NORMAL" -> productExpiration.expirationDate.gt(sevenDaysLater)
            else -> null
        }
    }
}
