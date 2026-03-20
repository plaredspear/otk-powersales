package com.otoki.internal.common.repository

import com.otoki.internal.branch.dto.response.BranchResponse
import com.otoki.internal.sap.entity.QEmployee.employee
import com.otoki.internal.sap.entity.Employee
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

    override fun findAllEmployeeNumbers(): List<String> {
        return queryFactory
            .select(employee.employeeNumber)
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
                employee.employeeNumber.containsIgnoreCase(keyword)
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
