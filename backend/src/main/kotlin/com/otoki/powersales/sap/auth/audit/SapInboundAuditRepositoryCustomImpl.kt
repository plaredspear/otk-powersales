package com.otoki.powersales.sap.auth.audit

import com.otoki.powersales.sap.auth.audit.QSapInboundAudit.Companion.sapInboundAudit
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import java.time.LocalDateTime

open class SapInboundAuditRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : SapInboundAuditRepositoryCustom {

    override fun search(
        clientId: String?,
        eventType: String?,
        endpoint: String?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        pageable: Pageable,
    ): Page<SapInboundAudit> {
        val builder = BooleanBuilder()
        if (!clientId.isNullOrBlank()) builder.and(sapInboundAudit.clientId.eq(clientId))
        if (!eventType.isNullOrBlank()) builder.and(sapInboundAudit.eventType.eq(eventType))
        if (!endpoint.isNullOrBlank()) builder.and(sapInboundAudit.endpoint.eq(endpoint))
        if (from != null) builder.and(sapInboundAudit.createdAt.goe(from))
        if (to != null) builder.and(sapInboundAudit.createdAt.lt(to))

        val content = queryFactory
            .selectFrom(sapInboundAudit)
            .where(builder)
            .orderBy(sapInboundAudit.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(sapInboundAudit.count())
            .from(sapInboundAudit)
            .where(builder)

        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }

    override fun findLatestByEndpointAndClientAndEvent(
        endpoint: String,
        clientId: String,
        eventType: String,
        pageable: Pageable,
    ): List<SapInboundAudit> {
        return queryFactory
            .selectFrom(sapInboundAudit)
            .where(
                sapInboundAudit.endpoint.eq(endpoint),
                sapInboundAudit.clientId.eq(clientId),
                sapInboundAudit.eventType.eq(eventType),
            )
            .orderBy(sapInboundAudit.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()
    }
}
