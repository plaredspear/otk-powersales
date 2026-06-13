package com.otoki.powersales.domain.sales.repository

import com.otoki.powersales.domain.foundation.account.entity.QAccount.Companion.account
import com.otoki.powersales.domain.sales.entity.SalesProgressRateMaster
import com.otoki.powersales.domain.sales.entity.QSalesProgressRateMaster.Companion.salesProgressRateMaster
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class SalesProgressRateMasterRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : SalesProgressRateMasterRepositoryCustom {

    override fun searchForAdmin(
        policyPredicate: Predicate,
        keyword: String?,
        targetYear: String?,
        targetMonth: String?,
        pageable: Pageable
    ): Page<SalesProgressRateMaster> {
        val builder = BooleanBuilder()

        // isDeleted 는 nullable (SF IsDeleted 적재) — false 또는 NULL 만 노출.
        builder.and(salesProgressRateMaster.isDeleted.isFalse.or(salesProgressRateMaster.isDeleted.isNull))
        builder.and(policyPredicate)

        if (!keyword.isNullOrBlank()) {
            val lowerPattern = "%${keyword.lowercase()}%"
            builder.and(
                salesProgressRateMaster.name.lower().like(lowerPattern)
                    .or(account.name.lower().like(lowerPattern))
            )
        }

        if (!targetYear.isNullOrBlank()) {
            builder.and(salesProgressRateMaster.targetYear.eq(targetYear))
        }

        if (!targetMonth.isNullOrBlank()) {
            builder.and(salesProgressRateMaster.targetMonth.eq(targetMonth))
        }

        val content = queryFactory
            .selectFrom(salesProgressRateMaster)
            // 거래처명/지점명/코드/유형 컬럼(SF Formula 동등) N+1 방지 — account lookup fetchJoin.
            .leftJoin(salesProgressRateMaster.account, account).fetchJoin()
            // policyPredicate 의 owner path (ownerUser.*) 가 implicit inner join 을 만들지 않도록 명시적 leftJoin.
            .leftJoin(salesProgressRateMaster.ownerUser)
            .where(builder)
            .orderBy(
                salesProgressRateMaster.targetYear.desc(),
                salesProgressRateMaster.targetMonth.desc(),
                salesProgressRateMaster.name.desc()
            )
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(salesProgressRateMaster.count())
            .from(salesProgressRateMaster)
            .leftJoin(salesProgressRateMaster.account, account)
            .leftJoin(salesProgressRateMaster.ownerUser)
            .where(builder)

        return PageableExecutionUtils.getPage(content, pageable) {
            countQuery.fetchOne() ?: 0L
        }
    }

    override fun findByIdWithRelations(id: Long): SalesProgressRateMaster? {
        return queryFactory
            .selectFrom(salesProgressRateMaster)
            .leftJoin(salesProgressRateMaster.account, account).fetchJoin()
            .where(salesProgressRateMaster.id.eq(id))
            .fetchOne()
    }

    override fun existsVisibleById(id: Long, policyPredicate: Predicate): Boolean {
        val where = BooleanBuilder()
            .and(salesProgressRateMaster.id.eq(id))
            .and(salesProgressRateMaster.isDeleted.isFalse.or(salesProgressRateMaster.isDeleted.isNull))
            .and(policyPredicate)

        return queryFactory
            .selectOne()
            .from(salesProgressRateMaster)
            .leftJoin(salesProgressRateMaster.ownerUser)
            .where(where)
            .fetchFirst() != null
    }
}
