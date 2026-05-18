package com.otoki.powersales.account.repository

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.entity.QAccount.Companion.account
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

    override fun findCoordinatesMissingAccounts(limit: Int): List<Account> {
        return queryFactory
            .selectFrom(account)
            .where(
                account.latitude.isNull.or(account.longitude.isNull),
                account.address1.isNotNull,
                account.externalKey.isNotNull,
                account.accountStatusName.eq(ACCOUNT_STATUS_ACTIVE)
            )
            .limit(limit.toLong())
            .fetch()
    }

    override fun existsActiveByName(name: String): Boolean {
        val found = queryFactory
            .selectOne()
            .from(account)
            .where(account.name.eq(name), notDeleted())
            .fetchFirst()
        return found != null
    }

    override fun findActiveById(id: Int): Account? {
        return queryFactory
            .selectFrom(account)
            .where(account.id.eq(id), notDeleted())
            .fetchOne()
    }

    override fun existsActiveByNameAndIdNot(name: String, id: Int): Boolean {
        val found = queryFactory
            .selectOne()
            .from(account)
            .where(account.name.eq(name), account.id.ne(id), notDeleted())
            .fetchFirst()
        return found != null
    }

    private fun notDeleted() = account.isDeleted.isNull.or(account.isDeleted.eq(false))

    companion object {
        private const val ACCOUNT_STATUS_ACTIVE = "거래"
    }
}
