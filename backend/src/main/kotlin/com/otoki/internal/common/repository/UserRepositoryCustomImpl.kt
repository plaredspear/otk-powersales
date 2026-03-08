package com.otoki.internal.common.repository

import com.otoki.internal.branch.dto.response.BranchResponse
import com.otoki.internal.sap.entity.QUser.user
import com.otoki.internal.sap.entity.User
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class UserRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : UserRepositoryCustom {

    override fun findDistinctBranches(): List<BranchResponse> {
        return queryFactory
            .select(
                Projections.constructor(
                    BranchResponse::class.java,
                    Expressions.cases()
                        .`when`(user.costCenterCode.isNotNull)
                        .then(user.costCenterCode)
                        .otherwise(user.orgName),
                    user.orgName
                )
            )
            .from(user)
            .where(
                user.orgName.isNotNull,
                user.isDeleted.isNull.or(user.isDeleted.isFalse)
            )
            .groupBy(user.orgName, user.costCenterCode)
            .orderBy(user.orgName.asc())
            .fetch()
    }

    override fun findAllEmployeeIds(): List<String> {
        return queryFactory
            .select(user.employeeId)
            .from(user)
            .fetch()
    }

    override fun findEmployees(
        status: String?,
        branchCodes: List<String>?,
        keyword: String?,
        appAuthority: String?,
        pageable: Pageable
    ): Page<User> {
        val where = BooleanBuilder()
            .and(user.isDeleted.isNull.or(user.isDeleted.isFalse))

        if (status != null) {
            where.and(user.status.eq(status))
        }
        if (branchCodes != null) {
            where.and(user.costCenterCode.`in`(branchCodes))
        }
        if (!keyword.isNullOrBlank()) {
            where.and(
                user.employeeId.containsIgnoreCase(keyword)
                    .or(user.name.containsIgnoreCase(keyword))
            )
        }
        if (appAuthority != null) {
            where.and(user.appAuthority.eq(appAuthority))
        }

        val content = queryFactory
            .selectFrom(user)
            .where(where)
            .orderBy(user.name.asc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(user.count())
            .from(user)
            .where(where)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }
}
