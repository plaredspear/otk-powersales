package com.otoki.internal.schedule.repository

import com.otoki.internal.sap.entity.QAccount.account
import com.otoki.internal.schedule.entity.DisplayWorkSchedule
import com.otoki.internal.schedule.entity.QDisplayWorkSchedule.displayWorkSchedule
import com.otoki.internal.sap.entity.QEmployee.employee
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import java.time.LocalDate

class DisplayWorkScheduleRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : DisplayWorkScheduleRepositoryCustom {

    override fun findDistinctAccountIdsByEmployeeIdAndStartDateBetween(
        employeeId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Int> {
        return queryFactory
            .select(displayWorkSchedule.account.id).distinct()
            .from(displayWorkSchedule)
            .where(
                displayWorkSchedule.employee.id.eq(employeeId),
                displayWorkSchedule.startDate.between(startDate, endDate)
            )
            .fetch()
            .filterNotNull()
    }

    override fun findDistinctStartDatesByEmployeeIdAndDateBetween(
        employeeId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<LocalDate> {
        return queryFactory
            .select(displayWorkSchedule.startDate).distinct()
            .from(displayWorkSchedule)
            .where(
                displayWorkSchedule.employee.id.eq(employeeId),
                displayWorkSchedule.startDate.between(startDate, endDate)
            )
            .orderBy(displayWorkSchedule.startDate.asc())
            .fetch()
            .filterNotNull()
    }

    override fun findDistinctAccountIdsByEmployeeIdAndDateRange(
        employeeId: Long,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<Int> {
        val dateCondition = BooleanBuilder()
            .or(displayWorkSchedule.startDate.goe(fromDate).and(displayWorkSchedule.startDate.lt(toDate)))
            .or(displayWorkSchedule.endDate.goe(fromDate).and(displayWorkSchedule.endDate.lt(toDate)))
            .or(displayWorkSchedule.endDate.isNull.and(displayWorkSchedule.startDate.lt(toDate)))

        return queryFactory
            .select(displayWorkSchedule.account.id).distinct()
            .from(displayWorkSchedule)
            .where(
                displayWorkSchedule.employee.id.eq(employeeId),
                dateCondition,
                displayWorkSchedule.account.id.isNotNull,
                isNotDeleted()
            )
            .fetch()
            .filterNotNull()
    }

    override fun findByEmployeeIdInAndNotDeleted(employeeIds: List<Long>): List<DisplayWorkSchedule> {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .where(
                displayWorkSchedule.employee.id.`in`(employeeIds),
                isNotDeleted()
            )
            .fetch()
    }

    override fun findScheduleList(
        employeeCode: String?,
        accountIds: List<Int>?,
        confirmed: Boolean?,
        typeOfWork3: String?,
        startDateFrom: LocalDate?,
        startDateTo: LocalDate?,
        pageable: Pageable
    ): Page<DisplayWorkSchedule> {
        val where = BooleanBuilder()
            .and(isNotDeleted())
            .and(buildEmployeeCodeCondition(employeeCode))
            .and(buildAccountIdsCondition(accountIds))
            .and(buildConfirmedCondition(confirmed))
            .and(buildTypeOfWork3Condition(typeOfWork3))
            .and(buildStartDateFromCondition(startDateFrom))
            .and(buildStartDateToCondition(startDateTo))

        val content = queryFactory
            .selectFrom(displayWorkSchedule)
            .leftJoin(displayWorkSchedule.employee).fetchJoin()
            .leftJoin(displayWorkSchedule.account).fetchJoin()
            .where(where)
            .orderBy(displayWorkSchedule.startDate.desc(), displayWorkSchedule.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(displayWorkSchedule.count())
            .from(displayWorkSchedule)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    override fun findByEmployeeAndStartDate(employeeId: Long, startDate: LocalDate): List<DisplayWorkSchedule> {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .leftJoin(displayWorkSchedule.account).fetchJoin()
            .where(
                displayWorkSchedule.employee.id.eq(employeeId),
                displayWorkSchedule.startDate.eq(startDate)
            )
            .fetch()
    }

    override fun findByEmployeeAndAccountAndStartDate(
        employeeId: Long,
        accountId: Int,
        startDate: LocalDate
    ): DisplayWorkSchedule? {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .where(
                displayWorkSchedule.employee.id.eq(employeeId),
                displayWorkSchedule.account.id.eq(accountId),
                displayWorkSchedule.startDate.eq(startDate)
            )
            .fetchOne()
    }

    override fun findByEmployeeAndStartDateBetween(
        employeeId: Long,
        start: LocalDate,
        end: LocalDate
    ): List<DisplayWorkSchedule> {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .leftJoin(displayWorkSchedule.account).fetchJoin()
            .where(
                displayWorkSchedule.employee.id.eq(employeeId),
                displayWorkSchedule.startDate.between(start, end)
            )
            .fetch()
    }

    override fun findByEmployeeIdsAndAccountIds(
        employeeIds: List<Long>,
        accountIds: List<Int>
    ): List<DisplayWorkSchedule> {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .leftJoin(displayWorkSchedule.employee).fetchJoin()
            .leftJoin(displayWorkSchedule.account).fetchJoin()
            .where(
                displayWorkSchedule.employee.id.`in`(employeeIds),
                displayWorkSchedule.account.id.`in`(accountIds)
            )
            .fetch()
    }

    override fun findConfirmedByDateRangeAndAccountIds(
        monthEnd: LocalDate,
        monthStart: LocalDate,
        accountIds: List<Int>
    ): List<DisplayWorkSchedule> {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .where(
                displayWorkSchedule.confirmed.eq(true),
                displayWorkSchedule.startDate.loe(monthEnd),
                displayWorkSchedule.endDate.goe(monthStart),
                displayWorkSchedule.account.id.`in`(accountIds)
            )
            .fetch()
    }

    override fun existsConfirmedByEmployeeAndAccountAndDate(
        employeeId: Long,
        accountId: Int,
        workingDate: LocalDate
    ): Boolean {
        val result = queryFactory
            .selectOne()
            .from(displayWorkSchedule)
            .where(
                displayWorkSchedule.employee.id.eq(employeeId),
                displayWorkSchedule.account.id.eq(accountId),
                displayWorkSchedule.confirmed.eq(true),
                isNotDeleted(),
                displayWorkSchedule.startDate.loe(workingDate),
                displayWorkSchedule.endDate.goe(workingDate)
            )
            .fetchFirst()
        return result != null
    }

    override fun findConfirmedValidByEmployeeAndDate(
        employeeId: Long,
        date: LocalDate
    ): List<DisplayWorkSchedule> {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .leftJoin(displayWorkSchedule.account).fetchJoin()
            .where(
                displayWorkSchedule.employee.id.eq(employeeId),
                displayWorkSchedule.confirmed.eq(true),
                isNotDeleted(),
                displayWorkSchedule.startDate.loe(date),
                displayWorkSchedule.endDate.goe(date)
                    .or(displayWorkSchedule.endDate.isNull)
            )
            .fetch()
    }

    private fun buildEmployeeCodeCondition(employeeCode: String?): BooleanExpression? {
        if (employeeCode.isNullOrBlank()) return null
        val matchingIds = JPAExpressions
            .select(employee.id)
            .from(employee)
            .where(employee.employeeCode.containsIgnoreCase(employeeCode))
        return displayWorkSchedule.employee.id.`in`(matchingIds)
    }

    private fun buildAccountIdsCondition(accountIds: List<Int>?): BooleanExpression? {
        if (accountIds == null) return null
        if (accountIds.isEmpty()) return displayWorkSchedule.account.id.eq(-1) // no match
        return displayWorkSchedule.account.id.`in`(accountIds)
    }

    private fun buildConfirmedCondition(confirmed: Boolean?): BooleanExpression? {
        return confirmed?.let { displayWorkSchedule.confirmed.eq(it) }
    }

    private fun buildTypeOfWork3Condition(typeOfWork3: String?): BooleanExpression? {
        if (typeOfWork3.isNullOrBlank()) return null
        return displayWorkSchedule.typeOfWork3.eq(typeOfWork3)
    }

    private fun buildStartDateFromCondition(startDateFrom: LocalDate?): BooleanExpression? {
        return startDateFrom?.let { displayWorkSchedule.startDate.goe(it) }
    }

    private fun buildStartDateToCondition(startDateTo: LocalDate?): BooleanExpression? {
        return startDateTo?.let { displayWorkSchedule.startDate.loe(it) }
    }

    private fun isNotDeleted(): BooleanExpression {
        return displayWorkSchedule.isDeleted.isNull.or(displayWorkSchedule.isDeleted.eq(false))
    }
}
