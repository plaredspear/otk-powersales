package com.otoki.powersales.account.repository

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.entity.QAccount.Companion.account
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class AccountRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : AccountRepositoryCustom {

    override fun findAllAccessibleByPolicy(
        policyPredicate: Predicate,
        keyword: String?,
        abcType: String?,
        accountStatusName: String?,
        applyPromotionFilter: Boolean,
        pageable: Pageable,
    ): Page<Account> {
        val builder = BooleanBuilder()

        builder.and(notDeleted())
        builder.and(policyPredicate)
        // SF AccId__c.lookupFilter 는 Promotion 거래처 선택 Lookup 에만 존재 — 메인 거래처 탭 listView
        // (AllAccounts=Everything) 에는 미적용. 따라서 lookup 진입점만 AND 합성.
        if (applyPromotionFilter) {
            builder.and(promotionLookupFilter())
        }

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

    override fun findAccessibleByPolicyAndId(policyPredicate: Predicate, id: Long): Account? {
        return queryFactory
            .selectFrom(account)
            .where(
                notDeleted(),
                policyPredicate,
                account.id.eq(id)
            )
            .fetchOne()
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

    override fun findActiveById(id: Long): Account? {
        return queryFactory
            .selectFrom(account)
            .where(account.id.eq(id), notDeleted())
            .fetchOne()
    }

    override fun existsActiveByNameAndIdNot(name: String, id: Long): Boolean {
        val found = queryFactory
            .selectOne()
            .from(account)
            .where(account.name.eq(name), account.id.ne(id), notDeleted())
            .fetchFirst()
        return found != null
    }

    override fun findByBranchCodeInAndExternalKeyIn(
        branchCodes: Collection<String>,
        externalKeys: Collection<String>
    ): List<Account> {
        if (branchCodes.isEmpty() || externalKeys.isEmpty()) return emptyList()
        return queryFactory
            .selectFrom(account)
            .where(
                account.branchCode.`in`(branchCodes),
                account.externalKey.`in`(externalKeys)
            )
            .fetch()
    }

    private fun notDeleted() = account.isDeleted.isNull.or(account.isDeleted.eq(false))

    /**
     * SF `DKRetail__Promotion__c.AccId__c.lookupFilter` 동등 비즈니스 필터.
     *
     * booleanFilter `1 AND 2 AND (3 OR (4 AND 5))` 원본:
     * 1. AccountGroup__c equals 1000,1010
     * 2. AccountGroup__c notEqual ""
     * 3. AccountStatusName__c notEqual "폐업"
     * 4. Distribution__c notEqual ""
     * 5. AccountStatusName__c equals "폐업"
     *
     * 정규화: `accountGroup ∈ {1000,1010} AND (accountStatusName != '폐업' OR distribution NON-EMPTY)`
     * (조건 2 는 1 에 흡수)
     */
    private fun promotionLookupFilter() = account.accountGroup.`in`(ACCOUNT_GROUP_SALES_VALUES)
        .and(
            account.accountStatusName.ne(ACCOUNT_STATUS_CLOSED)
                .or(account.accountStatusName.isNull)
                .or(
                    account.distribution.isNotNull
                        .and(account.distribution.ne(""))
                )
        )

    companion object {
        private const val ACCOUNT_STATUS_ACTIVE = "거래"
        private const val ACCOUNT_STATUS_CLOSED = "폐업"
        private val ACCOUNT_GROUP_SALES_VALUES = listOf("1000", "1010")
    }
}
