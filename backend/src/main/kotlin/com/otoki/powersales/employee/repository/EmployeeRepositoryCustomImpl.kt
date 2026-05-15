package com.otoki.powersales.employee.repository

import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.employee.entity.QEmployee.Companion.employee
import com.otoki.powersales.employee.entity.QEmployeeInfo.Companion.employeeInfo
import com.otoki.powersales.employee.entity.Employee
import com.querydsl.core.BooleanBuilder
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

    override fun findWithEmployeeInfoByCostCenterCodeAndRole(
        costCenterCode: String,
        role: UserRole
    ): List<Employee> {
        return queryFactory
            .selectFrom(employee)
            .leftJoin(employee.employeeInfo, employeeInfo).fetchJoin()
            .where(
                employee.costCenterCode.eq(costCenterCode),
                employee.role.eq(role)
            )
            .fetch()
    }

    override fun findActiveWomenByCostCenterCodes(costCenterCodes: List<String>?): List<Employee> {
        val builder = BooleanBuilder()
        builder.and(employee.role.eq(UserRole.WOMAN))
        builder.and(employee.appLoginActive.isTrue)
        builder.and(employee.isDeleted.isNull.or(employee.isDeleted.isFalse))
        if (!costCenterCodes.isNullOrEmpty()) {
            builder.and(employee.costCenterCode.`in`(costCenterCodes))
        }
        return queryFactory
            .selectFrom(employee)
            .leftJoin(employee.employeeInfo, employeeInfo).fetchJoin()
            .where(builder)
            .orderBy(employee.name.asc())
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
        role: UserRole?,
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
        if (role != null) {
            where.and(employee.role.eq(role))
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

    override fun resetAgreementFlagForActiveConsents(): Long {
        return queryFactory
            .update(employee)
            .set(employee.agreementFlag, false)
            .where(employee.agreementFlag.isTrue)
            .execute()
    }
}
