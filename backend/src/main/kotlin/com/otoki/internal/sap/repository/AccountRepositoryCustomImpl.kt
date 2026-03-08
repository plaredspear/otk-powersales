package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.QAccount.account
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class AccountRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : AccountRepositoryCustom {

    override fun searchForAdmin(
        keyword: String?,
        abcType: String?,
        branchCodes: List<String>?,
        accountStatusName: String?,
        pageable: Pageable
    ): Page<Account> {
        val builder = BooleanBuilder()

        builder.and(account.isDeleted.isNull.or(account.isDeleted.eq(false)))

        if (!keyword.isNullOrBlank()) {
            val lowerPattern = "%${keyword.lowercase()}%"
            builder.and(
                account.externalKey.lower().like(lowerPattern)
                    .or(account.name.lower().like(lowerPattern))
            )
        }

        if (!abcType.isNullOrBlank()) {
            builder.and(account.abcType.eq(abcType))
        }

        if (branchCodes != null) {
            builder.and(account.branchCode.`in`(branchCodes))
        }

        if (!accountStatusName.isNullOrBlank()) {
            builder.and(account.accountStatusName.eq(accountStatusName))
        }

        val content = queryFactory
            .selectFrom(account)
            .where(builder)
            .orderBy(account.name.asc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(account.count())
            .from(account)
            .where(builder)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }
}
