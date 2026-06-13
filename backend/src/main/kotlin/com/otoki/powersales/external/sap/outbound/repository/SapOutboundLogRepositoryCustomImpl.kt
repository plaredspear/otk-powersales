package com.otoki.powersales.external.sap.outbound.repository

import com.otoki.powersales.external.sap.outbound.entity.SapOutboundLog
import com.otoki.powersales.external.sap.outbound.entity.QSapOutboundLog.Companion.sapOutboundLog
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import java.time.LocalDateTime

open class SapOutboundLogRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : SapOutboundLogRepositoryCustom {

    override fun search(
        interfaceId: String?,
        resultCode: String?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        pageable: Pageable,
    ): Page<SapOutboundLog> {
        val builder = BooleanBuilder()
        if (!interfaceId.isNullOrBlank()) builder.and(sapOutboundLog.interfaceId.eq(interfaceId))
        if (!resultCode.isNullOrBlank()) builder.and(sapOutboundLog.resultCode.eq(resultCode))
        if (from != null) builder.and(sapOutboundLog.requestedAt.goe(from))
        if (to != null) builder.and(sapOutboundLog.requestedAt.lt(to))

        val content = queryFactory
            .selectFrom(sapOutboundLog)
            .where(builder)
            .orderBy(sapOutboundLog.requestedAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(sapOutboundLog.count())
            .from(sapOutboundLog)
            .where(builder)

        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }
}
