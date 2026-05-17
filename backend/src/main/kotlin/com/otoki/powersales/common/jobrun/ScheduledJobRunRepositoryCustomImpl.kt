package com.otoki.powersales.common.jobrun

import com.otoki.powersales.common.jobrun.QScheduledJobRun.Companion.scheduledJobRun
import com.querydsl.core.BooleanBuilder
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

open class ScheduledJobRunRepositoryCustomImpl(
    private val queryFactory: JPAQueryFactory
) : ScheduledJobRunRepositoryCustom {

    @Transactional
    override fun deleteByStartedAtBefore(threshold: LocalDateTime): Long {
        return queryFactory
            .delete(scheduledJobRun)
            .where(scheduledJobRun.startedAt.lt(threshold))
            .execute()
    }

    override fun search(
        jobName: String?,
        status: String?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        pageable: Pageable,
    ): Page<ScheduledJobRun> {
        val builder = BooleanBuilder()
        if (!jobName.isNullOrBlank()) builder.and(scheduledJobRun.jobName.eq(jobName))
        if (!status.isNullOrBlank()) builder.and(scheduledJobRun.status.eq(status))
        if (from != null) builder.and(scheduledJobRun.startedAt.goe(from))
        if (to != null) builder.and(scheduledJobRun.startedAt.lt(to))

        val content = queryFactory
            .selectFrom(scheduledJobRun)
            .where(builder)
            .orderBy(scheduledJobRun.startedAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val countQuery = queryFactory
            .select(scheduledJobRun.count())
            .from(scheduledJobRun)
            .where(builder)

        return PageableExecutionUtils.getPage(content, pageable) { countQuery.fetchOne() ?: 0L }
    }

    override fun countByStatusWithin(from: LocalDateTime, to: LocalDateTime): Map<String, Long> {
        val rows = queryFactory
            .select(scheduledJobRun.status, scheduledJobRun.count())
            .from(scheduledJobRun)
            .where(scheduledJobRun.startedAt.goe(from).and(scheduledJobRun.startedAt.lt(to)))
            .groupBy(scheduledJobRun.status)
            .fetch()

        return rows.associate { tuple ->
            val statusValue = tuple.get(scheduledJobRun.status) ?: ScheduledJobRun.STATUS_RUNNING
            val count = tuple.get(scheduledJobRun.count()) ?: 0L
            statusValue to count
        }
    }

    override fun findDistinctJobNames(): List<String> {
        return queryFactory
            .select(scheduledJobRun.jobName)
            .from(scheduledJobRun)
            .distinct()
            .orderBy(scheduledJobRun.jobName.asc())
            .fetch()
            .filterNotNull()
    }
}
