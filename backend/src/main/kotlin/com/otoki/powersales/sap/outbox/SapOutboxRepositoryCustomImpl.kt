package com.otoki.powersales.sap.outbox

import com.otoki.powersales.sap.outbox.QSapOutbox.Companion.sapOutbox
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils

class SapOutboxRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : SapOutboxRepositoryCustom {

    override fun findPendingOrRetry(pageable: Pageable): List<SapOutbox> {
        return queryFactory
            .selectFrom(sapOutbox)
            .where(pendingOrRetry())
            .orderBy(sapOutbox.createdAt.asc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
    }

    override fun pagePendingOrRetry(pageable: Pageable): Page<SapOutbox> {
        val content = queryFactory
            .selectFrom(sapOutbox)
            .where(pendingOrRetry())
            .orderBy(sapOutbox.createdAt.asc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(sapOutbox.count())
            .from(sapOutbox)
            .where(pendingOrRetry())

        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }

    private fun pendingOrRetry() =
        sapOutbox.status.`in`(SapOutbox.STATUS_PENDING, SapOutbox.STATUS_RETRY)
}
