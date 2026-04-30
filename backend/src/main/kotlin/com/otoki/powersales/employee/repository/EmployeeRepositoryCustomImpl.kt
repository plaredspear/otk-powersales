package com.otoki.powersales.employee.repository

import com.otoki.powersales.common.dto.response.BranchResponse
import com.otoki.powersales.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.employee.entity.QEmployeeInfo.Companion.employeeInfo
import com.otoki.powersales.employee.entity.Employee
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class EmployeeRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : EmployeeRepositoryCustom {

    override fun findWithEmployeeInfoByEmployeeCode(employeeCode: String): Employee? {
        return queryFactory
            .selectFrom(employee)
            .leftJoin(employee.employeeInfo, employeeInfo).fetchJoin()
            .where(employee.employeeCode.eq(employeeCode))
            .fetchOne()
    }

    override fun findWithEmployeeInfoById(id: Long): Employee? {
        return queryFactory
            .selectFrom(employee)
            .leftJoin(employee.employeeInfo, employeeInfo).fetchJoin()
            .where(employee.id.eq(id))
            .fetchOne()
    }

    override fun findWithEmployeeInfoByStatus(status: String): List<Employee> {
        return queryFactory
            .selectFrom(employee)
            .leftJoin(employee.employeeInfo, employeeInfo).fetchJoin()
            .where(employee.status.eq(status))
            .fetch()
    }

    override fun findWithEmployeeInfoByCostCenterCodeInAndStatus(
        costCenterCodes: List<String>,
        status: String
    ): List<Employee> {
        return queryFactory
            .selectFrom(employee)
            .leftJoin(employee.employeeInfo, employeeInfo).fetchJoin()
            .where(
                employee.costCenterCode.`in`(costCenterCodes),
                employee.status.eq(status)
            )
            .fetch()
    }

    override fun findWithEmployeeInfoByCostCenterCodeAndAppAuthority(
        costCenterCode: String,
        appAuthority: String
    ): List<Employee> {
        return queryFactory
            .selectFrom(employee)
            .leftJoin(employee.employeeInfo, employeeInfo).fetchJoin()
            .where(
                employee.costCenterCode.eq(costCenterCode),
                employee.appAuthority.eq(appAuthority)
            )
            .fetch()
    }

    override fun findDistinctBranches(): List<BranchResponse> {
        return queryFactory
            .select(
                Projections.constructor(
                    BranchResponse::class.java,
                    Expressions.cases()
                        .`when`(employee.costCenterCode.isNotNull)
                        .then(employee.costCenterCode)
                        .otherwise(employee.orgName),
                    employee.orgName
                )
            )
            .from(employee)
            .where(
                employee.orgName.isNotNull,
                employee.isDeleted.isNull.or(employee.isDeleted.isFalse)
            )
            .groupBy(employee.orgName, employee.costCenterCode)
            .orderBy(employee.orgName.asc())
            .fetch()
    }

    override fun findAllEmployeeCodes(): List<String> {
        return queryFactory
            .select(employee.employeeCode)
            .from(employee)
            .fetch()
    }

    override fun findEmployees(
        status: String?,
        branchCodes: List<String>?,
        keyword: String?,
        appAuthority: String?,
        pageable: Pageable
    ): Page<Employee> {
        val where = BooleanBuilder()
            .and(employee.isDeleted.isNull.or(employee.isDeleted.isFalse))

        if (status != null) {
            where.and(employee.status.eq(status))
        }
        if (branchCodes != null) {
            where.and(employee.costCenterCode.`in`(branchCodes))
        }
        if (!keyword.isNullOrBlank()) {
            where.and(
                employee.employeeCode.containsIgnoreCase(keyword)
                    .or(employee.name.containsIgnoreCase(keyword))
            )
        }
        if (appAuthority != null) {
            where.and(employee.appAuthority.eq(appAuthority))
        }

        val content = queryFactory
            .selectFrom(employee)
            .where(where)
            .orderBy(employee.name.asc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(employee.count())
            .from(employee)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }
}
