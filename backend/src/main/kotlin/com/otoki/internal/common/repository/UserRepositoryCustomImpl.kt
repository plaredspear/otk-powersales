package com.otoki.internal.common.repository

import com.otoki.internal.branch.dto.response.BranchResponse
import com.otoki.internal.sap.entity.QUser.user
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory

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
}
