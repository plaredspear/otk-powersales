package com.otoki.powersales.external.common.outboundlog.repository

import com.otoki.powersales.external.common.outboundlog.entity.ExternalApiLog
import com.otoki.powersales.external.common.outboundlog.entity.QExternalApiLog.Companion.externalApiLog
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import java.time.LocalDateTime

open class ExternalApiLogRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory,
) : ExternalApiLogRepositoryCustom {

    override fun search(
        targetSystem: String?,
        endpointKey: String?,
        success: Boolean?,
        httpMethod: String?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        pageable: Pageable,
    ): Page<ExternalApiLog> {
        val builder = BooleanBuilder()
        if (!targetSystem.isNullOrBlank()) builder.and(externalApiLog.targetSystem.eq(targetSystem))
        if (!endpointKey.isNullOrBlank()) builder.and(externalApiLog.endpointKey.eq(endpointKey))
        if (success != null) builder.and(externalApiLog.success.eq(success))
        if (!httpMethod.isNullOrBlank()) builder.and(externalApiLog.httpMethod.eq(httpMethod))
        if (from != null) builder.and(externalApiLog.requestedAt.goe(from))
        if (to != null) builder.and(externalApiLog.requestedAt.lt(to))

        val content = queryFactory
            .selectFrom(externalApiLog)
            .where(builder)
            .orderBy(externalApiLog.requestedAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(externalApiLog.count())
            .from(externalApiLog)
            .where(builder)

        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }
}
