package com.otoki.powersales.platform.common.jobrun

import com.otoki.powersales.platform.common.jobrun.QScheduledJobRun.Companion.scheduledJobRun
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

    override fun aggregateByJobNameWithin(
        jobNames: Collection<String>,
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<JobRunAggregate> {
        if (jobNames.isEmpty()) return emptyList()

        val windowPredicate = scheduledJobRun.jobName.`in`(jobNames)
            .and(scheduledJobRun.startedAt.goe(from))
            .and(scheduledJobRun.startedAt.lt(to))

        // status 별 카운트 (jobName x status group-by). 마지막 실행은 별도 조회로 합산.
        val statusRows = queryFactory
            .select(scheduledJobRun.jobName, scheduledJobRun.status, scheduledJobRun.count())
            .from(scheduledJobRun)
            .where(windowPredicate)
            .groupBy(scheduledJobRun.jobName, scheduledJobRun.status)
            .fetch()

        // jobName 별 가장 최근(startedAt DESC) 실행 시각 + 상태. 윈도우 내 대상 잡의 전체 row 를
        // DESC 로 fetch 한 뒤 잡별 first(=최신) 만 취한다 (대상 잡이 소수라 in-memory fold 로 충분).
        val lastRuns = queryFactory
            .select(scheduledJobRun.jobName, scheduledJobRun.startedAt, scheduledJobRun.status)
            .from(scheduledJobRun)
            .where(windowPredicate)
            .orderBy(scheduledJobRun.startedAt.desc())
            .fetch()
            .fold(HashMap<String, Pair<LocalDateTime, String>>()) { acc, tuple ->
                val name = tuple.get(scheduledJobRun.jobName) ?: return@fold acc
                if (!acc.containsKey(name)) {
                    val started = tuple.get(scheduledJobRun.startedAt)
                    val status = tuple.get(scheduledJobRun.status)
                    if (started != null && status != null) acc[name] = started to status
                }
                acc
            }

        data class Counts(
            var total: Long = 0,
            var success: Long = 0,
            var failure: Long = 0,
            var skipped: Long = 0,
            var running: Long = 0,
        )

        val countsByJob = HashMap<String, Counts>()
        for (tuple in statusRows) {
            val name = tuple.get(scheduledJobRun.jobName) ?: continue
            val status = tuple.get(scheduledJobRun.status) ?: continue
            val count = tuple.get(scheduledJobRun.count()) ?: 0L
            val counts = countsByJob.getOrPut(name) { Counts() }
            counts.total += count
            when (status) {
                ScheduledJobRun.STATUS_SUCCESS -> counts.success += count
                ScheduledJobRun.STATUS_FAILURE -> counts.failure += count
                ScheduledJobRun.STATUS_SKIPPED -> counts.skipped += count
                ScheduledJobRun.STATUS_RUNNING -> counts.running += count
            }
        }

        return countsByJob.map { (name, counts) ->
            val last = lastRuns[name]
            JobRunAggregate(
                jobName = name,
                totalCount = counts.total,
                successCount = counts.success,
                failureCount = counts.failure,
                skippedCount = counts.skipped,
                runningCount = counts.running,
                lastStartedAt = last?.first,
                lastStatus = last?.second,
            )
        }
    }
}
