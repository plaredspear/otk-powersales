package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.DisplayWorkSchedule
import com.otoki.internal.schedule.entity.QDisplayWorkSchedule.displayWorkSchedule
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import java.time.LocalDate

class DisplayWorkScheduleRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : DisplayWorkScheduleRepositoryCustom {

    override fun findDistinctAccountIdsByEmployeeNumberAndStartDateBetween(
        employeeNumber: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Int> {
        return queryFactory
            .select(displayWorkSchedule.accountId).distinct()
            .from(displayWorkSchedule)
            .where(
                displayWorkSchedule.employeeNumber.eq(employeeNumber),
                displayWorkSchedule.startDate.between(startDate, endDate)
            )
            .fetch()
            .filterNotNull()
    }

    override fun findDistinctStartDatesByEmployeeNumberAndDateBetween(
        employeeNumber: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<LocalDate> {
        return queryFactory
            .select(displayWorkSchedule.startDate).distinct()
            .from(displayWorkSchedule)
            .where(
                displayWorkSchedule.employeeNumber.eq(employeeNumber),
                displayWorkSchedule.startDate.between(startDate, endDate)
            )
            .orderBy(displayWorkSchedule.startDate.asc())
            .fetch()
            .filterNotNull()
    }

    override fun findDistinctAccountIdsBySfidAndDateRange(
        sfid: String,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<Int> {
        val dateCondition = BooleanBuilder()
            // startDate가 범위 이내
            .or(displayWorkSchedule.startDate.goe(fromDate).and(displayWorkSchedule.startDate.lt(toDate)))
            // endDate가 범위 이내
            .or(displayWorkSchedule.endDate.goe(fromDate).and(displayWorkSchedule.endDate.lt(toDate)))
            // endDate IS NULL이고 startDate가 범위 종료 전
            .or(displayWorkSchedule.endDate.isNull.and(displayWorkSchedule.startDate.lt(toDate)))

        return queryFactory
            .select(displayWorkSchedule.accountId).distinct()
            .from(displayWorkSchedule)
            .where(
                displayWorkSchedule.employeeNumber.eq(sfid),
                dateCondition,
                displayWorkSchedule.accountId.isNotNull,
                isNotDeleted()
            )
            .fetch()
            .filterNotNull()
    }

    override fun findByEmployeeNumberInAndNotDeleted(employeeNumbers: List<String>): List<DisplayWorkSchedule> {
        return queryFactory
            .selectFrom(displayWorkSchedule)
            .where(
                displayWorkSchedule.employeeNumber.`in`(employeeNumbers),
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

    private fun buildEmployeeCodeCondition(employeeCode: String?): BooleanExpression? {
        if (employeeCode.isNullOrBlank()) return null
        return displayWorkSchedule.employeeNumber.containsIgnoreCase(employeeCode)
    }

    private fun buildAccountIdsCondition(accountIds: List<Int>?): BooleanExpression? {
        if (accountIds == null) return null
        if (accountIds.isEmpty()) return displayWorkSchedule.accountId.eq(-1) // no match
        return displayWorkSchedule.accountId.`in`(accountIds)
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
