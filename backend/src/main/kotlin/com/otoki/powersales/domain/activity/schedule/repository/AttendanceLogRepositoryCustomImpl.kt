package com.otoki.powersales.domain.activity.schedule.repository

import com.otoki.powersales.domain.activity.schedule.dto.request.AdminAttendanceLogSearchRequest
import com.otoki.powersales.domain.activity.schedule.entity.AttendanceLog
import com.otoki.powersales.domain.org.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.domain.activity.schedule.entity.QAttendanceLog.Companion.attendanceLog
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

open class AttendanceLogRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : AttendanceLogRepositoryCustom {

    override fun searchByFilter(filter: AdminAttendanceLogSearchRequest, pageable: Pageable): Page<AttendanceLog> {
        val where = BooleanBuilder()

        filter.employeeId?.let { where.and(attendanceLog.employeeId.eq(it)) }
        filter.accountId?.let { where.and(attendanceLog.accountId.eq(it)) }
        filter.attendanceType?.let { where.and(attendanceLog.attendanceType.eq(it)) }
        filter.attendanceDateFrom?.let {
            where.and(attendanceLog.attendanceDate.goe(
                it.atStartOfDay()
            ))
        }
        filter.attendanceDateTo?.let {
            where.and(attendanceLog.attendanceDate.lt(
                it.plusDays(1).atStartOfDay()
            ))
        }

        filter.keyword?.takeIf { it.isNotBlank() }?.let { keyword ->
            val employeeIds = queryFactory
                .select(employee.id)
                .from(employee)
                .where(
                    employee.name.containsIgnoreCase(keyword)
                        .or(employee.employeeCode.containsIgnoreCase(keyword))
                )
                .fetch()
            if (employeeIds.isEmpty()) {
                where.and(attendanceLog.id.isNull)
            } else {
                where.and(attendanceLog.employeeId.`in`(employeeIds))
            }
        }

        val content = queryFactory
            .selectFrom(attendanceLog)
            .where(where)
            .orderBy(attendanceLog.attendanceDate.desc(), attendanceLog.id.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(attendanceLog.count())
            .from(attendanceLog)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }
}
