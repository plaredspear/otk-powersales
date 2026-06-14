package com.otoki.powersales.schedule.repository

import com.otoki.powersales.domain.org.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.schedule.dto.request.AdminAttendInfoSearchRequest
import com.otoki.powersales.schedule.entity.AttendInfo
import com.otoki.powersales.schedule.entity.QAttendInfo.Companion.attendInfo
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import java.time.format.DateTimeFormatter

open class AttendInfoRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : AttendInfoRepositoryCustom {

    override fun searchByFilter(filter: AdminAttendInfoSearchRequest, pageable: Pageable): Page<AttendInfo> {
        val where = BooleanBuilder()

        filter.employeeCode?.takeIf { it.isNotBlank() }?.let {
            where.and(attendInfo.employeeCode.eq(it))
        }
        filter.attendType?.takeIf { it.isNotBlank() }?.let {
            where.and(attendInfo.attendType.eq(it))
        }
        filter.status?.takeIf { it.isNotBlank() }?.let {
            where.and(attendInfo.status.equalsIgnoreCase(it))
        }
        filter.startDateFrom?.let {
            where.and(attendInfo.startDate.goe(it.format(DATE_FORMAT)))
        }
        filter.startDateTo?.let {
            where.and(attendInfo.startDate.loe(it.format(DATE_FORMAT)))
        }
        filter.keyword?.takeIf { it.isNotBlank() }?.let { keyword ->
            // 사원명 LIKE — employee join via employee_code
            val employeeCodes = queryFactory
                .select(employee.employeeCode)
                .from(employee)
                .where(employee.name.containsIgnoreCase(keyword))
                .fetch()
            if (employeeCodes.isEmpty()) {
                // 일치 사원 없음 → 결과 empty 강제
                where.and(attendInfo.id.isNull)
            } else {
                where.and(attendInfo.employeeCode.`in`(employeeCodes))
            }
        }
        filter.employeeId?.let { employeeId ->
            val employeeCodeOpt = queryFactory
                .select(employee.employeeCode)
                .from(employee)
                .where(employee.id.eq(employeeId))
                .fetchFirst()
            if (employeeCodeOpt == null) {
                where.and(attendInfo.id.isNull)
            } else {
                where.and(attendInfo.employeeCode.eq(employeeCodeOpt))
            }
        }

        val content = queryFactory
            .selectFrom(attendInfo)
            .where(where)
            .orderBy(attendInfo.startDate.desc(), attendInfo.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(attendInfo.count())
            .from(attendInfo)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    companion object {
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
